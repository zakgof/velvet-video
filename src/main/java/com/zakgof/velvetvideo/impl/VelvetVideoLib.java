package com.zakgof.velvetvideo.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zakgof.velvetvideo.Direction;
import com.zakgof.velvetvideo.IDecodedPacket;
import com.zakgof.velvetvideo.IDecoderVideoStream;
import com.zakgof.velvetvideo.IDemuxer;
import com.zakgof.velvetvideo.IEncoderVideoStream;
import com.zakgof.velvetvideo.IMuxer;
import com.zakgof.velvetvideo.IMuxerBuilder;
import com.zakgof.velvetvideo.IMuxerProperties;
import com.zakgof.velvetvideo.IRemuxerVideoStream;
import com.zakgof.velvetvideo.ISeekableInput;
import com.zakgof.velvetvideo.ISeekableOutput;
import com.zakgof.velvetvideo.IVelvetVideoLib;
import com.zakgof.velvetvideo.IVideoEncoderBuilder;
import com.zakgof.velvetvideo.IVideoFrame;
import com.zakgof.velvetvideo.IVideoRemuxerBuilder;
import com.zakgof.velvetvideo.IVideoStreamProperties;
import com.zakgof.velvetvideo.VelvetVideoException;
import com.zakgof.velvetvideo.impl.jnr.AVCodec;
import com.zakgof.velvetvideo.impl.jnr.AVCodecContext;
import com.zakgof.velvetvideo.impl.jnr.AVDictionaryEntry;
import com.zakgof.velvetvideo.impl.jnr.AVFormatContext;
import com.zakgof.velvetvideo.impl.jnr.AVFrame;
import com.zakgof.velvetvideo.impl.jnr.AVIOContext;
import com.zakgof.velvetvideo.impl.jnr.AVOutputFormat;
import com.zakgof.velvetvideo.impl.jnr.AVPacket;
import com.zakgof.velvetvideo.impl.jnr.AVPixelFormat;
import com.zakgof.velvetvideo.impl.jnr.AVStream;
import com.zakgof.velvetvideo.impl.jnr.LibAVCodec;
import com.zakgof.velvetvideo.impl.jnr.LibAVFormat;
import com.zakgof.velvetvideo.impl.jnr.LibAVFormat.ICustomAvioCallback;
import com.zakgof.velvetvideo.impl.jnr.LibAVUtil;
import com.zakgof.velvetvideo.impl.middle.Filters;
import com.zakgof.velvetvideo.impl.middle.FrameHolder;

import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.byref.PointerByReference;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

public class VelvetVideoLib implements IVelvetVideoLib {

    private static final int AVIO_CUSTOM_BUFFER_SIZE = 32768;

    private static final int CODEC_FLAG_GLOBAL_HEADER  = 1 << 22;

    private static final int AVMEDIA_TYPE_VIDEO = 0;
    private static final int AVMEDIA_TYPE_AUDIO = 1;


    private static final int  AVSEEK_FLAG_BACKWARD =1; ///< seek backward
    private static final int  AVSEEK_FLAG_BYTE     =2; ///< seeking based on position in bytes
    private static final int  AVSEEK_FLAG_ANY      =4; ///< seek to any frame, even non-keyframes
    private static final int  AVSEEK_FLAG_FRAME    =8;



    public static final int AVERROR_EOF = -541478725;
    public static final int AVERROR_EAGAIN = -11;
    private static final long AVNOPTS_VALUE = -9223372036854775808L;

    // private static final Logger ffmpegLogger = LoggerFactory.getLogger("velvet-video.ffmpeg");

    private final LibAVUtil libavutil = JNRHelper.load(LibAVUtil.class, "avutil-56");
    private final LibAVCodec libavcodec = JNRHelper.load(LibAVCodec.class, "avcodec-58");
    private final LibAVFormat libavformat = JNRHelper.load(LibAVFormat.class, "avformat-58");

    private int checkcode(int code) {
    	return libavutil.checkcode(code);
    }

    @Override
    public List<String> codecs(Direction dir) {
    	return libavcodec.codecs(dir);
    }

    @Override
    public IVideoEncoderBuilder videoEncoder(String codec) {
        return new VideoEncoderBuilderImpl(codec);
    }

    @Override
    public IVideoRemuxerBuilder videoRemux(IDecoderVideoStream decoder) {
    	return new VideoRemuxerBuilderImpl(decoder);
    }

    private String defaultName(AVStream avstream, int index) {
        AVDictionaryEntry entry = libavutil.av_dict_get(avstream.metadata.get(), "handler_name", null, 0);
        if (entry != null) {
            String name = entry.value.get();
            if (!name.equals("VideoHandler")) {
                return name;
            }
        }
        return "video" + index;
    }

    private void initCustomAvio(boolean read, AVFormatContext formatCtx, ICustomAvioCallback callback) {
        Pointer buffer = libavutil.av_malloc(AVIO_CUSTOM_BUFFER_SIZE + 64); // TODO free buffer
        AVIOContext avioCtx = libavformat.avio_alloc_context(buffer, AVIO_CUSTOM_BUFFER_SIZE, read ? 0 : 1, null, read ? callback : null, read ? null : callback, callback);
        int flagz = formatCtx.ctx_flags.get();
        formatCtx.ctx_flags.set(LibAVFormat.AVFMT_FLAG_CUSTOM_IO | flagz);
        formatCtx.pb.set(avioCtx); // TODO free avioCtx
    }

    private AVFormatContext createMuxerFormatContext(String format, Map<String, String> metadata) {
        AVOutputFormat outputFmt = libavformat.av_guess_format(format, null, null);
        if (outputFmt == null) {
            throw new VelvetVideoException("Unsupported format: " + format);
        }
        PointerByReference ctxptr = new PointerByReference();
        checkcode(libavformat.avformat_alloc_output_context2(ctxptr, outputFmt, null, null));
        AVFormatContext ctx = JNRHelper.struct(AVFormatContext.class, ctxptr.getValue());
        Pointer[] metadataPtr = libavutil.createDictionary(metadata);
        ctx.metadata.set(metadataPtr[0]);
        return ctx;
    }

    private abstract class MuxerVideoStreamImpl implements AutoCloseable {
    	final Consumer<AVPacket> output;
    	final AVPacket packet;
        AVStream stream;
		int codecTimeBaseNum;
		int codecTimeBaseDen;
		int streamIndex;
		long nextPts = 0;
		int defaultFrameDuration;

		MuxerVideoStreamImpl(Consumer<AVPacket> output) {
			this.output = output;
			this.packet = libavcodec.av_packet_alloc();
		}

		public void init() {
			this.defaultFrameDuration = codecTimeBaseNum * stream.time_base.den.get() / codecTimeBaseDen / stream.time_base.num.get();
			this.streamIndex = stream.index.get();
		}

		public void flush() {
		}

		@Override
		public void close() {
			libavcodec.av_packet_free(new Pointer[] {Struct.getMemory(packet)});
		}

    }

    private class RemuxerVideoStreamImpl extends MuxerVideoStreamImpl implements IRemuxerVideoStream {

    	public RemuxerVideoStreamImpl(VideoRemuxerBuilderImpl builder, AVFormatContext formatCtx, Consumer<AVPacket> output) {
			super(output);
    		this.stream = libavformat.avformat_new_stream(formatCtx, null);
			DemuxerImpl.DecoderVideoStreamImpl decoderImpl = (DemuxerImpl.DecoderVideoStreamImpl) builder.decoder;
			checkcode(libavcodec.avcodec_parameters_copy(stream.codecpar.get(), decoderImpl.avstream.codecpar.get()));
        	stream.codecpar.get().codec_tag.set(0);
        	int timeBaseNum = builder.timebaseNum == null ? decoderImpl.codecCtx.time_base.num.get() * decoderImpl.codecCtx.ticks_per_frame.get(): builder.timebaseNum;
        	int timeBaseDen = builder.timebaseDen == null ? decoderImpl.codecCtx.time_base.den.get() : builder.timebaseDen;
        	stream.time_base.num.set(timeBaseNum);
            stream.time_base.den.set(timeBaseDen);
            this.codecTimeBaseNum = timeBaseNum;
            this.codecTimeBaseDen = timeBaseDen;
		}

    	@Override
		public void writeRaw(byte[] packetData) {

			// TODO !!! free
			Pointer pointer = Runtime.getSystemRuntime().getMemoryManager().allocateDirect(packetData.length);
			pointer.put(0, packetData, 0, packetData.length);
            libavcodec.av_init_packet(packet);
            packet.data.set(pointer);
            packet.size.set(packetData.length);

            packet.stream_index.set(streamIndex);

            packet.pts.set(nextPts);
			packet.duration.set(defaultFrameDuration);
			nextPts += defaultFrameDuration;
            output.accept(packet);
		}

    }

    private class EncoderVideoStreamImpl extends MuxerVideoStreamImpl implements IEncoderVideoStream {

    	private final Logger logEncoder = LoggerFactory.getLogger("velvet-video.encoder");
        private final AVCodecContext codecCtx;

        private final AVCodec codec;
        private final Pointer[] codecOpts;

        private FrameHolder frameHolder;
		private boolean codecOpened;
		private Filters filters;
		private final String filterString;

        public EncoderVideoStreamImpl(VideoEncoderBuilderImpl builder, AVFormatContext formatCtx, Consumer<AVPacket> output) {
        	super(output);
			this.codecOpts = libavutil.createDictionary(builder.params);
			this.filterString = builder.filter;
            this.codec = libavcodec.avcodec_find_encoder_by_name(builder.codec);
            if (this.codec == null && builder.decoder == null) {
                throw new VelvetVideoException("Unknown video codec: " + builder.codec);
            }
            this.stream = libavformat.avformat_new_stream(formatCtx, codec);

            this.codecCtx = libavcodec.avcodec_alloc_context3(codec);
            if ((formatCtx.ctx_flags.get() & LibAVFormat.AVFMT_GLOBALHEADER) != 0) {
            	codecCtx.flags.set(codecCtx.flags.get() | CODEC_FLAG_GLOBAL_HEADER);
            }
            codecCtx.codec_id.set(codec.id.get());
            codecCtx.codec_type.set(codec.type.get());
            codecCtx.bit_rate.set(builder.bitrate == null ? 400000 : builder.bitrate);
            codecCtx.time_base.num.set(builder.timebaseNum == null ? 1 : builder.timebaseNum);
            codecCtx.time_base.den.set(builder.timebaseDen == null ? 30 : builder.timebaseDen);
            int firstFormat = codec.pix_fmts.get().getInt(0);
            codecCtx.pix_fmt.set(firstFormat); // TODO ?
            codecCtx.width.set(builder.width == null ? 1 : builder.width);
            codecCtx.height.set(builder.height == null ? 1 : builder.height);
            if (builder.enableExperimental) {
            	codecCtx.strict_std_compliance.set(-2);
            }
            Pointer[] metadata = libavutil.createDictionary(builder.metadata);
            stream.metadata.set(metadata[0]);
            checkcode(libavcodec.avcodec_parameters_from_context(stream.codecpar.get(), codecCtx));

            stream.time_base.num.set(codecCtx.time_base.num.get());
            stream.time_base.den.set(codecCtx.time_base.den.get());
            stream.index.set(formatCtx.nb_streams.get() - 1);
            stream.id.set(formatCtx.nb_streams.get() - 1);

        	this.codecTimeBaseNum = codecCtx.time_base.num.get();
            this.codecTimeBaseDen = codecCtx.time_base.den.get();
        }

		@Override
        public void encode(BufferedImage image) {
			encode(image, defaultFrameDuration);
		}

		@Override
        public void encode(BufferedImage image, int duration) {
            int width = image.getWidth();
            int height = image.getHeight();

            if (!this.codecOpened) {
            	codecCtx.width.set(width);
                codecCtx.height.set(height);
                checkcode(libavcodec.avcodec_parameters_from_context(stream.codecpar.get(), codecCtx));
                checkcode(libavcodec.avcodec_open2(codecCtx, codecCtx.codec.get(), codecOpts));
                codecOpened = true;
                if (filterString != null)
                	this.filters = new Filters(VelvetVideoLib.this, codecCtx, filterString);
            } else {
            	if (codecCtx.width.get() != width || codecCtx.height.get() != height) {
            		throw new VelvetVideoException("Image dimensions do not match, expected " + codecCtx.width.get() + "x" + codecCtx.height.get());
            	}
            }

            if (frameHolder == null) {
            	frameHolder = new FrameHolder(width, height, AVPixelFormat.avformatOf(image.getType()), codecCtx.pix_fmt.get(), true);
            }

            AVFrame frame = frameHolder.setPixels(image);
            frame.extended_data.set(frame.data[0].getMemory());
            frame.pts.set(nextPts);
            nextPts += duration;
            submitFrame(frame, duration);
        }

		private void submitFrame(AVFrame frame, int duration) {
			if (filters == null) {
				encodeFrame(frame, duration);
			} else {
				filters.submitFrame(frame, frameHolder.frame, filteredFrame -> encodeFrame(filteredFrame, duration));
			}
		}

        private void encodeFrame(AVFrame frame, int duration) {
        	logEncoder.atDebug().log(() -> frame == null ? "stream " + streamIndex + ": flush" :  "stream " + streamIndex + ": send frame for encoding, PTS=" + frame.pts.get());
            checkcode(libavcodec.avcodec_send_frame(codecCtx, frame));
            for (;;) {
                libavcodec.av_init_packet(packet);
                packet.data.set((Pointer) null);
                packet.size.set(0);

                int res = libavcodec.avcodec_receive_packet(codecCtx, packet);
                if (res == AVERROR_EAGAIN || res == AVERROR_EOF)
                    break;
                checkcode(res);
                packet.stream_index.set(streamIndex);
        		if (packet.duration.get() == 0 || packet.duration.get() == AVNOPTS_VALUE) {
    				packet.duration.set(duration);
    			}

                logEncoder.atDebug()
                	.addArgument(() -> packet.pts.get())
                	.addArgument(() -> packet.dts.get())
                	.addArgument(() -> packet.duration.get())
                	.addArgument(() -> packet.size.get())
                	.log(() -> "encoder: returned packet  PTS/DTS: {}/{}, duration={} ,{} bytes");

                output.accept(packet);
            }
        }

        @Override
		public void flush() {

        	submitFrame(null, defaultFrameDuration);

        }



		@Override
		public void close() {
			super.close();

			libavcodec.avcodec_close(codecCtx);
			libavcodec.avcodec_free_context(new Pointer[] { Struct.getMemory(codecCtx) });

		}

    }

    @Override
    public IMuxerBuilder muxer(String format) {
        return new MuxerBuilderImpl(format);
    }

    private class MuxerBuilderImpl implements IMuxerBuilder {

        private final String format;
        private final List<Object> videoStreamBuilders = new ArrayList<>(); // TODO: SO UGLY :(
        private final Map<String, String> metadata = new LinkedHashMap<>();

        public MuxerBuilderImpl(String format) {
            this.format = format;
        }

        @Override
        public IMuxerBuilder videoEncoder(IVideoEncoderBuilder encoderBuilder) {
        	videoStreamBuilders.add(encoderBuilder);
            return this;
        }

		@Override
		public IMuxerBuilder videoRemuxer(IDecoderVideoStream decoder) {
			return videoRemuxer(new VideoRemuxerBuilderImpl(decoder));
		}

		@Override
		public IMuxerBuilder videoRemuxer(IVideoRemuxerBuilder remuxerBuilder) {
			videoStreamBuilders.add(remuxerBuilder);
			return this;
		}

        @Override
        public IMuxerBuilder metadata(String key, String value) {
            metadata.put(key, value);
            return this;
        }

        @Override
        public IMuxer build(ISeekableOutput output) {
            return new MuxerImpl(output, this);
        }

        @Override
        public IMuxer build(File outputFile) {
            try {
                FileSeekableOutput output = new FileSeekableOutput(new FileOutputStream(outputFile));
                return new MuxerImpl(output, this);
            } catch (FileNotFoundException e) {
                throw new VelvetVideoException(e);
            }
        }


    }

    private class MuxerImpl implements IMuxer {

    	private final Logger logMuxer = LoggerFactory.getLogger("velvet-video.muxer");

        private final LibAVFormat libavformat;
        private final List<MuxerVideoStreamImpl> videoStreams; // TODO: this is so ugly
        private final ISeekableOutput output;
        private final AVFormatContext formatCtx;
        private final IOCallback callback;

        private MuxerImpl(ISeekableOutput output, MuxerBuilderImpl builder) {

            this.libavformat = JNRHelper.load(LibAVFormat.class, "avformat-58");
            this.output = output;
            this.formatCtx = createMuxerFormatContext(builder.format, builder.metadata);
            this.callback = new IOCallback();
            initCustomAvio(false, formatCtx, callback);

            Consumer<AVPacket> packetStream = packet -> {
            	logMuxer.atDebug()
            		.addArgument(() -> packet.pts.get())
            		.addArgument(() -> packet.dts.get())
            		.addArgument(() -> packet.duration.get())
            		.addArgument(() -> packet.size.get())
            		.log("writing packet PTS/DTS = {}/{}, duration={}, {} bytes");
            	checkcode(libavformat.av_write_frame(formatCtx, packet));
            };

            this.videoStreams = builder.videoStreamBuilders.stream()
            	.map(videoStreamBuilder ->  {
            		if (videoStreamBuilder instanceof VideoEncoderBuilderImpl) {
            			return new EncoderVideoStreamImpl((VideoEncoderBuilderImpl)videoStreamBuilder, formatCtx, packetStream);
            		} else if (videoStreamBuilder instanceof VideoRemuxerBuilderImpl) {
            			return new RemuxerVideoStreamImpl((VideoRemuxerBuilderImpl)videoStreamBuilder, formatCtx, packetStream);
            		} else {
            			throw new VelvetVideoException("Unknown video stream builder type");
            		}
            	 })
            	.collect(Collectors.toList());

            checkcode(libavformat.avformat_write_header(formatCtx, null));

            // TODO: fix dis hack
            videoStreams.forEach(enc -> {
            	enc.init();
            });
        }

        @Override
        public IEncoderVideoStream videoEncoder(int index) {
        	if (index >= videoStreams.size())
        		throw new VelvetVideoException("No video stream found with index " + index);
            return (IEncoderVideoStream) videoStreams.get(index);
        }

        @Override
        public IRemuxerVideoStream videoRemuxer(int index) {
        	if (index >= videoStreams.size())
        		throw new VelvetVideoException("No video stream found with index " + index);
            return (IRemuxerVideoStream) videoStreams.get(index);

        }

		private class IOCallback implements ICustomAvioCallback {

			@Override
			public int read_packet(Pointer opaque, Pointer buf, int buf_size) {
				// TODO [low] perf: prealloc buffer
				byte[] bytes = new byte[buf_size];
				buf.get(0, bytes, 0, buf_size);
				output.write(bytes);
				return buf_size;
			}

            @Override
            public int seek(Pointer opaque, int offset, int whence) {
                // TODO [low] support other whence values
                if (whence != 0)
                    throw new IllegalArgumentException();
                output.seek(offset);
                return offset;
            }
        }

        @Override
        public void close() {
            // flush encoders
            for (MuxerVideoStreamImpl encoder : videoStreams) {
                encoder.flush();
                encoder.close();
            }
            // flush muxer
            do {
            	logMuxer.atDebug().log("flushing");
            } while (checkcode(libavformat.av_write_frame(formatCtx, null)) == 0);

            logMuxer.atDebug().log("writing trailer");
            checkcode(libavformat.av_write_trailer(formatCtx));
            // dispose resources
            // libavformat.avio_context_free(new PointerByReference(Struct.getMemory(avioCtx)));
            libavformat.avformat_free_context(formatCtx);
            output.close();
        }


    }

    @Override
    public IDemuxer demuxer(InputStream is) {
        return new DemuxerImpl((FileInputStream) is);
    }

    private class DemuxerImpl implements IDemuxer {

    	private final Logger logDemuxer = LoggerFactory.getLogger("velvet-video.demuxer");
        private final AVFormatContext formatCtx;
        private final ISeekableInput input;
        private final IOCallback callback;
        private final List<DecoderVideoStreamImpl> streams = new ArrayList<>();
        private final AVPacket packet;
        private final Map<Integer, DecoderVideoStreamImpl> indexToVideoStream = new LinkedHashMap<>();
        private Flusher flusher;

        public DemuxerImpl(FileInputStream input) {
            this.input = new FileSeekableInput(input);

            this.packet = libavcodec.av_packet_alloc(); // TODO free

            formatCtx = libavformat.avformat_alloc_context();
            this.callback = new IOCallback();
            initCustomAvio(true, formatCtx, callback);

            PointerByReference ptrctx = new PointerByReference(Struct.getMemory(formatCtx));
            checkcode(libavformat.avformat_open_input(ptrctx, null, null, null));
            checkcode(libavformat.avformat_find_stream_info(formatCtx, null));

            long nb = formatCtx.nb_streams.get();
            Pointer pointer = formatCtx.streams.get();
            for (int i=0; i<nb; i++) {
                Pointer mem = pointer.getPointer(i * pointer.getRuntime().addressSize());
                AVStream avstream = JNRHelper.struct(AVStream.class, mem);
                if (avstream.codec.get().codec_type.get() == AVMEDIA_TYPE_VIDEO) {
                    avstream.codec.get().strict_std_compliance.set(-2);
                    DecoderVideoStreamImpl decoder = new DecoderVideoStreamImpl(avstream, defaultName(avstream, i));
                    streams.add(decoder);
                    indexToVideoStream.put(i, decoder);
                }
            }
        }

        private class IOCallback implements ICustomAvioCallback {

            @Override
            public int read_packet(Pointer opaque, Pointer buf, int buf_size) {
                byte[] bytes = new byte[buf_size];
                int bts;
                bts = input.read(bytes);
                if (bts > 0) {
                    buf.put(0, bytes, 0, bts);
                }
                return bts;
            }

            @Override
            public int seek(Pointer opaque, int offset, int whence) {

                final int SEEK_SET = 0;   /* set file offset to offset */
                final int SEEK_CUR = 1;   /* set file offset to current plus offset */
                final int SEEK_END = 2;   /* set file offset to EOF plus offset */
                final int AVSEEK_SIZE = 0x10000;   /* set file offset to EOF plus offset */

                if (whence == SEEK_SET)
                    input.seek(offset);
                else if (whence == SEEK_END)
                    input.seek(input.size() - offset);
                else if (whence == AVSEEK_SIZE)
                    return (int) input.size();
                else throw new VelvetVideoException("Unsupported seek operation " + whence);
                return offset;
            }

        }

        @Override
		public IDecodedPacket nextPacket() {
			libavcodec.av_init_packet(packet);
			packet.data.set((Pointer) null);
			packet.size.set(0);

			for (;;) {
				int res = libavformat.av_read_frame(formatCtx, packet);
				if (res == AVERROR_EAGAIN)
					continue;

				if (res == AVERROR_EOF || res == -1) {
					if (flusher == null) {
						flusher = new Flusher();
					}
					return flusher.flush();
				}
				checkcode(res);

				logDemuxer.atDebug()
					.addArgument(packet.stream_index.get())
					.addArgument(packet.pts.get())
					.addArgument(packet.dts.get())
				.log(() -> "read packet stream: {} PTS/DTS={}/{}");
				IDecodedPacket decodedPacket = decodePacket(packet);
				if (decodedPacket != null) {
					return decodedPacket;
				}
			}
		}

        @Override
        public Stream<IDecodedPacket> stream() {
        	// return Stream.generate(this::nextPacket).takeWhile(el -> el != null);
        	return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
        }

        @Override
        public Iterator<IDecodedPacket> iterator() {
        	return new Iterator<IDecodedPacket>() {

        		private IDecodedPacket next = nextPacket();

				@Override
				public boolean hasNext() {
					return next != null;
				}

				@Override
				public IDecodedPacket next() {
					if (next == null)
						throw new NoSuchElementException();
					IDecodedPacket ret = next;
					next = nextPacket();
					return ret;
				}
			};
        }

        /**
         * @return null means "PACKET HAS NO OUTPUT DATA, GET NEXT PACKET"
         */
        private IDecodedPacket decodePacket(AVPacket p) {
            int index = p.stream_index.get();
            DecoderVideoStreamImpl stream = indexToVideoStream.get(index);
            if (stream != null)
                return stream.decodePacket(p);
            logDemuxer.atWarn().addArgument(index).log("received packet of unknown stream {}");
            return new UnknownDecodedPacket(packet.bytes());
        }

        private class Flusher {

            private int streamIndex = 0;

            public IDecodedPacket flush() {
                for (;streamIndex<streams.size(); streamIndex++) {
                	logDemuxer.atDebug().addArgument(streamIndex).log(() -> "flushing demuxer stream={}");
                	DecoderVideoStreamImpl stream = streams.get(streamIndex);
                	IDecodedPacket packet = stream.decodePacket(null);
                    if (packet != null) {
                    	return packet;
                    }
                }
                return null;
            }
        }

        class DecoderVideoStreamImpl implements IDecoderVideoStream {

        	private final Logger logDecoder = LoggerFactory.getLogger("velvet-video.decoder");

            private final AVStream avstream;
            private final String name;
            private final AVCodecContext codecCtx;

            private FrameHolder frameHolder;
            private final int index;
            private long skipToPts = -1;

            public DecoderVideoStreamImpl(AVStream avstream, String name) {
                this.avstream = avstream;
                this.name = name;
                this.index = avstream.index.get();
                this.codecCtx = avstream.codec.get();
                AVCodec codec = libavcodec.avcodec_find_decoder(codecCtx.codec_id.get());
                checkcode(libavcodec.avcodec_open2(codecCtx, codec, null));
            }

            private IVideoFrame frameOf(BufferedImage bi) {
                long pts = frameHolder.frame.pts.get();
                if (pts == AVNOPTS_VALUE) {
                	pts = 0;
                }
				long nanostamp = pts * 1000000000L * avstream.time_base.num.get() / avstream.time_base.den.get();
                long duration = libavutil.av_frame_get_pkt_duration(frameHolder.frame);
                long nanoduration = duration * 1000000000L * avstream.time_base.num.get() / avstream.time_base.den.get();
                return new Frame(bi, nanostamp, nanoduration, this);
            }

            /**
             * @return null means "PACKET HAS NO OUTPUT DATA, GET NEXT PACKET"
             */
            IDecodedPacket decodePacket(AVPacket pack) {
                int res = libavcodec.avcodec_send_packet(codecCtx, pack);
                if (res != AVERROR_EOF && pack == null) {
                	// When flushing, ignore EOF until receive_frame is full flushed
                	checkcode(res);
                }

                if (frameHolder == null) {
                    this.frameHolder = new FrameHolder(codecCtx.width.get(), codecCtx.height.get(), codecCtx.pix_fmt.get(), AVPixelFormat.AV_PIX_FMT_BGR24, false);
                }
                for(;;) {
	                res = libavcodec.avcodec_receive_frame(codecCtx, frameHolder.frame);
	                if (res >=0) {
	                    long pts = frameHolder.frame.pts.get();
	                    logDecoder.atDebug().addArgument(pts).log("decoded frame pts={}");
	                    if (skipToPts != -1) {
	                    	if (pts == AVNOPTS_VALUE) {
	                    		throw new VelvetVideoException("Cannot seek when decoded packets have no PTS. Looks like neighter codec no container keep timing information.");
	                    	}
	                        if (pts < skipToPts) {
	                        	logDecoder.atDebug().addArgument(() -> skipToPts).log(" ...but need to skip more to pts={}");
	                            res = -11;
	                        } else if (pts > skipToPts) {
	                        	logDecoder.atWarn().addArgument(pts).addArgument(skipToPts).log(" ...unexpected position: PTS={} missed target PTS={}");
	                            // res = -11;
	                        }
	                    }
	                    if (res >= 0) {
	                        skipToPts = -1;
	                        return new DecodedVideoPacket(frameOf(frameHolder.getPixels()));
	                    }
	                } else {
	                	logDecoder.atDebug().addArgument(res).log("decoder: res={}");
	                }
	                if (pack == null && res == AVERROR_EAGAIN) {
	                	continue;
	                }
	                if (res == AVERROR_EOF || pack != null && res == AVERROR_EAGAIN) {
	                	return null;
	                }
	                checkcode(res);
                }
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public int index() {
            	return index;
            }

            @Override
            public Map<String, String> metadata() {
                Pointer dictionary = avstream.metadata.get();
                return libavutil.dictionaryToMap(dictionary);
            }

            @Override
            public IVideoStreamProperties properties() {
                int timebase_n = avstream.time_base.num.get();
                int timebase_d = avstream.time_base.den.get();
                long duration = avstream.duration.get() * 1000000000L * timebase_n / timebase_d;
                long frames = avstream.nb_frames.get();
                int width = codecCtx.width.get();
                int height = codecCtx.height.get();
                AVCodec codec = libavcodec.avcodec_find_decoder(codecCtx.codec_id.get());
                double framerate = (double)avstream.avg_frame_rate.num.get() / avstream.avg_frame_rate.den.get();
                return new VideoStreamProperties(codec.name.get(), framerate, duration, frames, width, height);
            }

            @Override
            public IDecoderVideoStream seek(long frameIndex) {
            	// TODO: this won't work for var-duration streams
                long cn = codecCtx.time_base.num.get();
                long cd = codecCtx.time_base.den.get();
                long defaultFrameDur = cn * avstream.time_base.den.get() * codecCtx.ticks_per_frame.get() / (cd * avstream.time_base.num.get());
                long pts = frameIndex * defaultFrameDur;
                logDecoder.atDebug().addArgument(() -> frameIndex).addArgument(() -> pts).log("seeking to frame {}, target pts={}");
                return seekToPts(pts);
            }

            @Override
            public IDecoderVideoStream seekNano(long nanostamp) {
                long pts = nanostamp * avstream.time_base.den.get() / avstream.time_base.num.get() / 1000000;
                logDecoder.atDebug().addArgument(() -> nanostamp).addArgument(() -> pts).log("seeking to t={} ns, target pts={}");
                return seekToPts(pts);
            }

			private IDecoderVideoStream seekToPts(long pts) {
				checkcode(libavformat.av_seek_frame(formatCtx, this.index, pts, AVSEEK_FLAG_FRAME | AVSEEK_FLAG_BACKWARD));
                libavcodec.avcodec_flush_buffers(codecCtx);
                this.skipToPts  = pts;
                flusher = null;
                return this;
			}

			@Override
			public IVideoFrame nextFrame() {
				IDecodedPacket packet;
				while((packet = nextPacket()) != null) {
					if (packet.isVideo() && packet.video().stream() == this) {
						return packet.video();
					}
				}
				return null;
			}

            @Override
            public byte[] nextRawPacket() {
            	 libavcodec.av_init_packet(packet);
                 packet.data.set((Pointer) null);
                 packet.size.set(0);

                 int res;
                 do {
                     res = libavformat.av_read_frame(formatCtx, packet);
                     logDemuxer.atDebug()
                        .addArgument(() -> packet.stream_index.get())
                        .addArgument(() -> packet.pts.get())
                        .addArgument(() -> packet.dts.get())
                        .log("read packet stream={}  PTS/DTS={}/{}");
                     if (res == AVERROR_EOF || res == -1) {
//                         if (flusher == null) {
//                             flusher = new Flusher();
//                         }
//                         res = flusher.flush(videoConsumer, audioConsumer);
                    	 // TODO !
                    	 return null;
                     } else {
                    	 return packet.bytes();
                     }
                 } while (res == AVERROR_EAGAIN);
            }

        }

        @Override
        public List<? extends IDecoderVideoStream> videos() {
            return streams;
        }

        @Override
        public IDecoderVideoStream video(int index) {
        	return streams.get(index);
        }

        @Override
        public Map<String, String> metadata() {
            Pointer dictionary = formatCtx.metadata.get();
            return libavutil.dictionaryToMap(dictionary);
        }

        @Override
        public IMuxerProperties properties() {
        	// TODO: how to get single format ?
        	return new MuxerProperties(formatCtx.iformat.get().name.get(), formatCtx.duration.get());
        }

        @Override
        public void close() {
            // TODO Auto-generated method stub

        }

        @Override
        public String toString() {
        	return "Demuxer " + properties();
        }

    }
}

@Accessors(fluent = true)
@Value
class Frame implements IVideoFrame {
    private final BufferedImage image;
    private final long nanostamp;
    private final long nanoduration;
    private final IDecoderVideoStream stream;

    @Override
	public String toString() {
    	return "Video frame t=" + nanostamp + " stream:" + stream.name();
    }
}

@Accessors(fluent = true)
@Value
@ToString
class MuxerProperties implements IMuxerProperties {
    private final String format;
    private final long duration;
}

@Accessors(fluent = true)
@Value
@ToString
class VideoStreamProperties implements IVideoStreamProperties {
    private final String codec;
    private final double framerate;
    private final long nanoduration;
    private final long frames;
    private final int width;
    private final int height;
}

@Accessors(fluent = true)
@RequiredArgsConstructor
class UnknownDecodedPacket implements IDecodedPacket {
	private final byte[] bytes;
}

@Accessors(fluent = true)
@RequiredArgsConstructor
class DecodedVideoPacket implements IDecodedPacket {
	private final IVideoFrame frame;

	@Override
	public IVideoFrame video() {
		return frame;
	}

	@Override
	public boolean isVideo() {
		return true;
	}
}
