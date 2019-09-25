package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zakgof.velvetvideo.FFMpegNative.AVCodec;
import com.zakgof.velvetvideo.FFMpegNative.AVCodecContext;
import com.zakgof.velvetvideo.FFMpegNative.AVDictionaryEntry;
import com.zakgof.velvetvideo.FFMpegNative.AVFormatContext;
import com.zakgof.velvetvideo.FFMpegNative.AVFrame;
import com.zakgof.velvetvideo.FFMpegNative.AVIOContext;
import com.zakgof.velvetvideo.FFMpegNative.AVOutputFormat;
import com.zakgof.velvetvideo.FFMpegNative.AVPacket;
import com.zakgof.velvetvideo.FFMpegNative.AVPixelFormat;
import com.zakgof.velvetvideo.FFMpegNative.AVStream;
import com.zakgof.velvetvideo.FFMpegNative.LibAVCodec;
import com.zakgof.velvetvideo.FFMpegNative.LibAVFormat;
import com.zakgof.velvetvideo.FFMpegNative.LibAVFormat.ICustomAvioCallback;
import com.zakgof.velvetvideo.FFMpegNative.LibAVUtil;
import com.zakgof.velvetvideo.FFMpegNative.LibSwScale;
import com.zakgof.velvetvideo.FFMpegNative.SwsContext;
import com.zakgof.velvetvideo.IVideoLib.IDecodedPacket;
import com.zakgof.velvetvideo.IVideoLib.IDecoderVideoStream;
import com.zakgof.velvetvideo.IVideoLib.IEncoder.IBuilder;
import com.zakgof.velvetvideo.IVideoLib.IFrame;
import com.zakgof.velvetvideo.IVideoLib.IMuxerProperties;
import com.zakgof.velvetvideo.IVideoLib.IVideoStreamProperties;

import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.Struct.int64_t;
import jnr.ffi.byref.PointerByReference;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

public class FFMpegVideoLib implements IVideoLib {
    
    private static final int AVIO_CUSTOM_BUFFER_SIZE = 32768;
    private static final int AVFMT_FLAG_CUSTOM_IO =  0x0080; 
    private static final int AVFMT_GLOBALHEADER = 0x0040;
    private static final int CODEC_FLAG_GLOBAL_HEADER  = 1 << 22;
    
    private static final int AVMEDIA_TYPE_VIDEO = 0;
    private static final int AVMEDIA_TYPE_AUDIO = 1;
    

    private static final int  AVSEEK_FLAG_BACKWARD =1; ///< seek backward
    private static final int  AVSEEK_FLAG_BYTE     =2; ///< seeking based on position in bytes
    private static final int  AVSEEK_FLAG_ANY      =4; ///< seek to any frame, even non-keyframes
    private static final int  AVSEEK_FLAG_FRAME    =8;
    
    private static final int AV_DICT_IGNORE_SUFFIX = 2; 
    
    private static final int AVERROR_EOF = -541478725;
    private static final int AVERROR_EAGAIN = -11;
    private static final long AVNOPTS_VALUE = -9223372036854775808L;
    
    private static final int ERROR_TEXT_BUFFER_SIZE = 512;

    private final Runtime runtime = Runtime.getSystemRuntime();
    
    private final LibAVUtil libavutil;
    private final LibSwScale libswscale;
    private final LibAVCodec libavcodec;
    private final LibAVFormat libavformat;

    private int checkcode(int code) {
        if (code < 0) {
			Pointer ptr = runtime.getMemoryManager().allocateDirect(ERROR_TEXT_BUFFER_SIZE); // TODO !!!
            libavutil.av_strerror(code, ptr, ERROR_TEXT_BUFFER_SIZE);
            byte[] bts = new byte[ERROR_TEXT_BUFFER_SIZE];
            ptr.get(0, bts, 0, ERROR_TEXT_BUFFER_SIZE);
            int len = 0;
            for (int i=0; i<ERROR_TEXT_BUFFER_SIZE; i++) if (bts[i] == 0) len = i;
            String s = new String(bts, 0, len);
            throw new VelvetVideoException("FFMPEG error " + code + " : "+ s);
        }
        return code;
    }

    public FFMpegVideoLib() {
        this.libavutil = JNRHelper.load(LibAVUtil.class, "avutil-56");
        this.libswscale = JNRHelper.load(LibSwScale.class, "swscale-5");
        this.libavcodec = JNRHelper.load(LibAVCodec.class, "avcodec-58");
        this.libavformat = JNRHelper.load(LibAVFormat.class, "avformat-58");
    }

    @Override
    public List<String> codecs(Direction dir) {
        PointerByReference ptr = new PointerByReference();
        AVCodec codec;
        List<String> codecs = new ArrayList<>();
        while ((codec = libavcodec.av_codec_iterate(ptr)) != null) {
            
            if (matches(codec, dir)) {            
                codecs.add(codec.name.get());
            }
        }
        return codecs;
    }

    private boolean matches(AVCodec codec, Direction dir) {
        switch (dir) {
            case Decode: return libavcodec.av_codec_is_decoder(codec) != 0;
            case Encode: return libavcodec.av_codec_is_encoder(codec) != 0;
            case All: return true;
        }
        return false;
    }

    @Override
    public IBuilder encoder(String codec) {
        return new EncoderBuilderImpl(codec);
    }
    
    private AVCodecContext createCodecContext(EncoderBuilderImpl builder) {
        AVCodec codec = libavcodec.avcodec_find_encoder_by_name(builder.codec);
        AVCodecContext ctx = libavcodec.avcodec_alloc_context3(codec);
        configureCodecContext(ctx, codec, builder);
        return ctx;
    }

    private void configureCodecContext(AVCodecContext ctx, AVCodec codec, EncoderBuilderImpl builder) {
        ctx.codec_id.set(codec.id.get());
        ctx.codec_type.set(codec.type.get());
        ctx.bit_rate.set(builder.bitrate);
        ctx.time_base.num.set(builder.timebaseNum);
        ctx.time_base.den.set(builder.timebaseDen);
        int firstFormat = codec.pix_fmts.get().getInt(0);
        ctx.pix_fmt.set(firstFormat); // TODO ?
        ctx.width.set(640); // TODO !!!
        ctx.height.set(480);
        if (builder.enableExperimental) {
            ctx.strict_std_compliance.set(-2);
        }
        Pointer[] opts = createDictionary(builder.params);
        checkcode(libavcodec.avcodec_open2(ctx, codec, opts));
        
    }

    private Pointer[] createDictionary(Map<String, String> map) {
        Pointer[] opts = new Pointer[1];
        for (Entry<String, String> entry : map.entrySet()) {
            libavutil.av_dict_set(opts, entry.getKey(), entry.getValue(), 0);
        }
        return opts;
    }

    private Map<String, String> dictionaryToMap(Pointer dictionary) {
        Map<String, String> metadata = new LinkedHashMap<>();
        AVDictionaryEntry entry = null;
        do {
            entry = libavutil.av_dict_get(dictionary, "", entry, AV_DICT_IGNORE_SUFFIX);
            if (entry != null) {
                metadata.put(entry.key.get(), entry.value.get());
            }
        } while (entry != null);
        return metadata;
    }
    
    private static byte[] extractPacketBytes(AVPacket packet) {
		byte[] raw = new byte[packet.size.get()];
		packet.data.get().get(0, raw, 0, raw.length);
		return raw;
	}

    private AVPixelFormat avformatOf(int type) {
        if (type == BufferedImage.TYPE_3BYTE_BGR) {
            return AVPixelFormat.AV_PIX_FMT_BGR24;
        } else {
            throw new VelvetVideoException("Unsupported BufferedImage type, only TYPE_3BYTE_BGR supported at the moment");
        }
    }

    private byte[] bytesOf(BufferedImage image) {
        Raster raster = image.getRaster();
        DataBuffer buffer = raster.getDataBuffer();
        if (buffer instanceof DataBufferByte) {
            return ((DataBufferByte) buffer).getData();
        }
        throw new VelvetVideoException("Unsupported image data buffer type");
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
        formatCtx.ctx_flags.set(AVFMT_FLAG_CUSTOM_IO | flagz);
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
        Pointer[] metadataPtr = createDictionary(metadata);
        ctx.metadata.set(metadataPtr[0]);
        return ctx;
    }

    private AVStream createVideoStream(AVFormatContext formatCtx, EncoderBuilderImpl builder) {
        AVCodec codec = libavcodec.avcodec_find_encoder_by_name(builder.codec);
        if (codec == null) {
            throw new VelvetVideoException("Unknown video codec: " + builder.codec);
        }
        AVStream stream = libavformat.avformat_new_stream(formatCtx, codec);
        
        AVCodecContext codecContext = stream.codec.get();
        if ((formatCtx.ctx_flags.get() & AVFMT_GLOBALHEADER) != 0) {
            codecContext.flags.set(codecContext.flags.get() | CODEC_FLAG_GLOBAL_HEADER);
        }
        configureCodecContext(codecContext, codec, builder);
                        
        stream.id.set(formatCtx.nb_streams.get() - 1);
        stream.time_base.den.set(builder.timebaseDen);
        stream.time_base.num.set(builder.timebaseNum);
        Pointer[] metadata = createDictionary(builder.metadata);
        stream.metadata.set(metadata[0]);
        return stream;
    }

    private class FrameHolder implements AutoCloseable {
    
        private final AVFrame frame;
        private final AVFrame biframe;
        private final SwsContext scaleCtx;
        private final int width;
        private final int height;
    
        // AVPixelFormat srcFormat = avformatOf(originalImageType);
        public FrameHolder(int width, int height, AVPixelFormat srcFormat, AVPixelFormat destFormat, boolean encode) {
            this.width = width;
            this.height = height;    
            this.frame = alloc(width, height, encode ? destFormat : srcFormat);
            this.biframe = alloc(width, height, encode ? srcFormat : destFormat);
            scaleCtx = libswscale.sws_getContext(width, height, srcFormat, width, height, destFormat, 0, 0, 0, 0);
        }
    
        private AVFrame alloc(int width, int height, AVPixelFormat format) {
            AVFrame f = libavutil.av_frame_alloc();
            f.width.set(width);
            f.height.set(height);
            f.pix_fmt.set(format);
            checkcode(libavutil.av_frame_get_buffer(f, 0));
            return f;
        }
    
        public AVFrame setPixels(BufferedImage image) {
            byte[] bytes = bytesOf(image);
            biframe.data[0].get().put(0, bytes, 0, bytes.length);
            checkcode(libswscale.sws_scale(scaleCtx, JNRHelper.ptr(biframe.data[0]), JNRHelper.ptr(biframe.linesize[0]), 0, height, 
                                           JNRHelper.ptr(frame.data[0]), JNRHelper.ptr(frame.linesize[0])));
            return frame;
        }
    
        public BufferedImage getPixels() {
            checkcode(libswscale.sws_scale(scaleCtx, JNRHelper.ptr(frame.data[0]), JNRHelper.ptr(frame.linesize[0]), 0, height, 
                                           JNRHelper.ptr(biframe.data[0]), JNRHelper.ptr(biframe.linesize[0])));
            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            byte[] bytes = bytesOf(bi);
            biframe.data[0].get().get(0,  bytes, 0, bytes.length);
            return bi;
            
        }
        
        public void close() {
           libavutil.av_frame_free(new AVFrame[] {frame});
           libavutil.av_frame_free(new AVFrame[] {biframe});
           libswscale.sws_freeContext(scaleCtx);
        }
    
    }

    private class EncoderBuilderImpl implements IBuilder {

        private final String codec;
        private int timebaseNum = 1;
        private int timebaseDen = 30;
        private int bitrate = 400000;
        private Map<String, String> params = new HashMap<>();
        private Map<String, String> metadata = new HashMap<>();
        private boolean enableExperimental;

        public EncoderBuilderImpl(String codec) {
            this.codec = codec;
        }

        @Override
        public IBuilder framerate(int framerate) {
            this.timebaseNum = 1;
            this.timebaseDen = framerate;
            return this;
        }

        @Override
        public IBuilder bitrate(int bitrate) {
            this.bitrate = bitrate;
            return this;
        }

        @Override
        public IBuilder param(String key, String value) {
            params.put(key, value);
            return this;
        }
        
        @Override
        public IBuilder metadata(String key, String value) {
            metadata.put(key, value);
            return this;
        }
        
        @Override
        public IBuilder enableExperimental() {
            this.enableExperimental = true;
            return this;
        }

//        private EncoderImpl build(IPacketStream stream) {
//            return new EncoderImpl(stream, this);      
//        }
//
//        @Override
//        public IEncoder build(OutputStream output) {
//            return build(new OutputPacketStream(output));
//        }

    }

    private class EncoderImpl implements IEncoder {
        
    	private final Logger logEncoder = LoggerFactory.getLogger("velvet-video.encoder");

        private final AVPacket packet;
        private final AVCodecContext codecCtx;

        private FrameHolder frameHolder;
        private final int containerTimeBaseNum;
        private final int containerTimeBaseDen;
        private final int streamIndex;
        private Consumer<AVPacket> output;

        private long lastKnownPts = AVNOPTS_VALUE;
        private long lastKnownDts = AVNOPTS_VALUE;

        
        private EncoderImpl(Consumer<AVPacket> output, EncoderBuilderImpl builder) {            
            this(output, createCodecContext(builder), builder.timebaseNum, builder.timebaseDen, 0);
        }
        
        private EncoderImpl(Consumer<AVPacket> output, AVCodecContext codecCtx, int containerTimeBaseNum, int containerTimeBaseDen, int streamIndex) {
            this.output = output;            
            this.codecCtx = codecCtx;
            this.containerTimeBaseNum = containerTimeBaseNum;
            this.containerTimeBaseDen = containerTimeBaseDen;
            this.packet = libavcodec.av_packet_alloc(); // TODO free
            this.streamIndex = streamIndex;
        }       

        @Override
        public void encode(BufferedImage image, long pts) {
            int width = image.getWidth();
            int height = image.getHeight();
            codecCtx.width.set(width);
            codecCtx.height.set(height);

            if (frameHolder == null) {
                frameHolder = new FrameHolder(width, height, avformatOf(image.getType()), codecCtx.pix_fmt.get(), true);
            }

            AVFrame frame = frameHolder.setPixels(image);
            if (pts >= 0) {
                frame.pts.set((int) pts);
            }

            frame.extended_data.set(frame.data[0].getMemory());
            encodeFrame(frame, packet);
        }

        private void encodeFrame(AVFrame frame, AVPacket packet) {
        	logEncoder.atDebug().log(() -> frame == null ? ">>> flush  the encoder" : ">>> send frame to encoder " + frame.pts.get());
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
                fixPacketPtsDts(packet);
                
                logEncoder.atDebug()
                	.addArgument(() -> packet.pts.get()) 
                	.addArgument(() -> packet.dts.get()) 
                	.log(() -> "<<< encoder returned packet  PTS/DTS: {}/{}");
                
                output.accept(packet);
            }
        }

        private void fixPacketPtsDts(AVPacket packet) {
        	if (packet.dts.get() == AVNOPTS_VALUE) {
        		if (lastKnownDts == AVNOPTS_VALUE) {
        			lastKnownDts = 0;
        		} else {
        			lastKnownDts++;
        		}
        		packet.dts.set(lastKnownDts);
        	}
        	if (packet.pts.get() == AVNOPTS_VALUE) {
        		if (lastKnownPts == AVNOPTS_VALUE) {
        			lastKnownPts = 0;
        		} else {
        			lastKnownPts++;
        		}
        		packet.pts.set(lastKnownDts);
        	}
            scaleTime(packet.dts);
            scaleTime(packet.pts);
        }

        private void scaleTime(int64_t ts) {
            long oldTs = ts.get();
            if (oldTs != AVNOPTS_VALUE) {
                long cn = codecCtx.time_base.num.get();
                long cd = codecCtx.time_base.den.get();
                long newTs = oldTs * cn * containerTimeBaseDen / cd / containerTimeBaseNum;
                ts.set(newTs);
            }
        }

        private void flush() {
            encodeFrame(null, packet);
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
            fixPacketPtsDts(packet);
            output.accept(packet);
		}

    }

    @Override
    public IMuxer.IBuilder muxer(String format) {
        return new MuxerBuilderImpl(format);
    }

    private class MuxerBuilderImpl implements IMuxer.IBuilder {

        private String format;
        private Map<String, EncoderBuilderImpl> videos = new LinkedHashMap<>();
        private Map<String, String> metadata = new LinkedHashMap<>();

        public MuxerBuilderImpl(String format) {
            this.format = format;
        }

        @Override
        public IMuxer.IBuilder video(String name, IEncoder.IBuilder encoderBuilder) {
            videos.put(name, (EncoderBuilderImpl) encoderBuilder);
            encoderBuilder.metadata("handler_name", name);
            return this;
        }
        
        @Override
        public IMuxer.IBuilder metadata(String key, String value) {
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
        private final Map<String, EncoderImpl> videoStreams;
        private final ISeekableOutput output;
        private AVFormatContext formatCtx;
        private IOCallback callback;
        
        private MuxerImpl(ISeekableOutput output, MuxerBuilderImpl builder) {
            
            this.libavformat = JNRHelper.load(LibAVFormat.class, "avformat-58");
            this.output = output;
            this.formatCtx = createMuxerFormatContext(builder.format, builder.metadata);
            this.callback = new IOCallback();
            initCustomAvio(false, formatCtx, callback);
            
            Map<String, AVStream> streams = builder.videos.entrySet().stream()
               .collect(Collectors.toMap(Entry::getKey, entry -> createVideoStream(formatCtx, (EncoderBuilderImpl)entry.getValue())));
         
            checkcode(libavformat.avformat_write_header(formatCtx, null));
            
            Consumer<AVPacket> packetStream = packet -> checkcode(libavformat.av_write_frame(formatCtx, packet));
            
            this.videoStreams = streams.entrySet().stream()
               .collect(Collectors.toMap(Entry::getKey, entry -> {
                   AVStream stream = entry.getValue();
                   AVCodecContext codecCtx = stream.codec.get();
                   return new EncoderImpl(packetStream, codecCtx, stream.time_base.num.get(), stream.time_base.den.get(), stream.index.get());
               }));
            
        }
        
        private class IOCallback implements ICustomAvioCallback {
            
            @Override
            public int read_packet(Pointer opaque, Pointer buf, int buf_size) {
                byte[] bytes = new byte[buf_size]; // TODO perf: prealloc buffer
                buf.get(0, bytes, 0, buf_size);
                output.write(bytes);
                return buf_size;
            }
            
            @Override
            public int seek(Pointer opaque, int offset, int whence) {
                // TODO !!! whence
                if (whence != 0) 
                    throw new IllegalArgumentException();
                output.seek(offset);
                return offset;
            }
        }

        @Override
        public void close() {
            // flush encoders
            for (EncoderImpl encoder : videoStreams.values()) {
                encoder.flush();
            }
            // flush muxer         
            checkcode(libavformat.av_write_frame(formatCtx, null));

            checkcode(libavformat.av_write_trailer(formatCtx));
            // dispose resources
            // libavformat.avio_context_free(new PointerByReference(Struct.getMemory(avioCtx)));
            libavformat.avformat_free_context(formatCtx);
            output.close();
        }

        @Override
        public IEncoder video(String name) {
            return videoStreams.get(name);
        }
        
    }
    
//    private interface IPacketStream extends AutoCloseable {
//        void send(AVPacket packet);
//        void close();
//    }
//    
//    private class OutputPacketStream implements IPacketStream {
//
//        private final OutputStream output;
//
//        public OutputPacketStream(OutputStream output) {
//            this.output = output;
//        }
//
//        @Override
//        public void send(AVPacket packet) {
//            byte[] bts = new byte[packet.size.get()]; // TODO perf: preallocate buffer
//            packet.data.get().get(0, bts, 0, bts.length);
//            try {
//                output.write(bts);
//            } catch (IOException e) {
//                throw new VelvetVideoException(e);
//            }
//        }
//
//        @Override
//        public void close() {
//            try {
//                output.close();
//            } catch (IOException e) {
//                throw new VelvetVideoException(e);
//            }
//        }
//    }

    @Override
    public IDemuxer demuxer(InputStream is) {
        return new DemuxerImpl((FileInputStream) is);
    }
    
    private class DemuxerImpl implements IDemuxer {
        
    	private final Logger logDemuxer = LoggerFactory.getLogger("velvet-video.demuxer");
        private AVFormatContext formatCtx;
        private ISeekableInput input;
        private IOCallback callback;
        private List<DecoderVideoStreamImpl> streams = new ArrayList<>();
        private AVPacket packet;
        private Map<Integer, DecoderVideoStreamImpl> indexToVideoStream = new LinkedHashMap<>();
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
				
				logDemuxer.atDebug().log(() -> "read packet stream=" + packet.stream_index.get() + "  PTS/DTS="
						+ packet.pts.get() + "/" + packet.dts.get());
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
        	return new Iterator<IVideoLib.IDecodedPacket>() {
        		
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

        @Override
        public boolean nextPacket(Consumer<IFrame> videoConsumer, Consumer<IAudioPacket> audioConsumer) {
        	IDecodedPacket packet = nextPacket();
        	if (packet == null) {
        		return false;
        	}
        	if (packet.isVideo()) {
        		videoConsumer.accept(packet.video());
        	}
        	return true;
        }

        /**
         * @return null means "PACKET HAS NO OUTPUT DATA, GET NEXT PACKET"
         */
        private IDecodedPacket decodePacket(AVPacket p) {
            int index = p.stream_index.get();
            DecoderVideoStreamImpl stream = indexToVideoStream.get(index);
            if (stream != null)
                return stream.decodePacket(p);
            logDemuxer.atWarn().addArgument(() -> index).log("WARNING: packet of unknown stream {}");
            return new UnknownDecodedPacket(extractPacketBytes(packet));
        }

        private class Flusher {
            
            private int streamIndex = 0;
            
            public IDecodedPacket flush() {
                for (;streamIndex<streams.size(); streamIndex++) {
                	DecoderVideoStreamImpl stream = streams.get(streamIndex);
                	IDecodedPacket packet = stream.decodePacket(null);
                    if (packet != null)
                    	return packet;
                }
                return null;
            }
        }

        private class DecoderVideoStreamImpl implements IDecoderVideoStream {

        	private final Logger logDecoder = LoggerFactory.getLogger("velvet-video.decoder");
            
            private final AVStream avstream;
            private final String name;
            private AVCodecContext codecCtx;
            
            private FrameHolder frameHolder;
            private int index;
            private long skipToPts = -1;
         
            public DecoderVideoStreamImpl(AVStream avstream, String name) {
                this.avstream = avstream;
                this.name = name;
                this.index = avstream.index.get();
                this.codecCtx = avstream.codec.get();
                AVCodec codec = libavcodec.avcodec_find_decoder(codecCtx.codec_id.get());
                checkcode(libavcodec.avcodec_open2(codecCtx, codec, null));
            }

            private IFrame frameOf(BufferedImage bi) {                
                long nanostamp = frameHolder.frame.pts.get() * 1000000000L * avstream.time_base.num.get() / avstream.time_base.den.get();
                return new Frame(bi, nanostamp, this);
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
                res = libavcodec.avcodec_receive_frame(codecCtx, frameHolder.frame);
                if (res >=0) {
                    long pts = frameHolder.frame.pts.get();
                    logDecoder.atDebug().addArgument(() -> pts).log("decoder: frame pts={}");
                    if (skipToPts != -1) {
                        if (pts < skipToPts) {
                        	logDecoder.atDebug().addArgument(() -> skipToPts).log(" ...but need to skip more to pts={}");
                            res = -11;
                        } else if (pts > skipToPts) {
                        	logDecoder.atDebug().addArgument(() -> skipToPts).log(" ...unexpected position but continue searching for pts={}");
                            res = -11;
                        }
                    }
                    if (res >= 0) {
                        skipToPts = -1;
                        return new DecodedVideoPacket(frameOf(frameHolder.getPixels()));
                    }
                } else {
                	logDecoder.atDebug().addArgument(res).log("decoder: res={}");
                }
                // Weird but ok. AVERROR_EOF on flush, AVERROR_EAGAIN on norm
                if (res == AVERROR_EOF || res == AVERROR_EAGAIN) {
                	return null;
                }
                checkcode(res);
                return null; // Unreachable
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public Map<String, String> metadata() {
                Pointer dictionary = avstream.metadata.get();
                return dictionaryToMap(dictionary);
            }
            
            @Override
            public IVideoStreamProperties properties() {
                int timebase_n = avstream.time_base.num.get();
                int timebase_d = avstream.time_base.den.get();
                long duration = avstream.duration.get() * 1000L * timebase_n / timebase_d;
                long frames = avstream.nb_frames.get();
                int width = codecCtx.width.get();
                int height = codecCtx.height.get();
                AVCodec codec = libavcodec.avcodec_find_decoder(codecCtx.codec_id.get());
                double framerate = (double)avstream.avg_frame_rate.num.get() / avstream.avg_frame_rate.den.get();
                return new VideoStreamProperties(codec.name.get(), framerate, duration, frames, width, height);
            }
            
            @Override
            public IDecoderVideoStream seek(long timestamp) {
                
                long cn = codecCtx.time_base.num.get();
                long cd = codecCtx.time_base.den.get();
                
                long pts = timestamp * cn * avstream.time_base.den.get() * codecCtx.ticks_per_frame.get() / cd / avstream.time_base.num.get();
                logDecoder.atDebug().addArgument(() -> timestamp).addArgument(() -> pts).log("seeking to frame t={} pts={}");
                checkcode(libavformat.av_seek_frame(formatCtx, this.index, pts, AVSEEK_FLAG_FRAME | AVSEEK_FLAG_BACKWARD));
                this.skipToPts  = pts;
                return this;
            }

			@Override
			public IFrame nextFrame() {
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
                    	 return extractPacketBytes(packet);
                     }                
                 } while (res == AVERROR_EAGAIN);
            }

			
            
        }
        
        @Override
        public List<? extends IDecoderVideoStream> videos() {
            return streams;
        }
        
        @Override
        public IDecoderVideoStream videoStream(String streamName) {
        	return streams.stream().filter(vs -> vs.name.equals(streamName)).findFirst().orElse(null);
        }
        
        @Override
        public Map<String, String> metadata() {
            Pointer dictionary = formatCtx.metadata.get();
            return dictionaryToMap(dictionary);
        }
        
        @Override
        public IMuxerProperties properties() {
        	return new MuxerProperties(formatCtx.iformat.get().name.get());
        }

        @Override
        public void close() {
            // TODO Auto-generated method stub
            
        }
        
    }
}

@Accessors(fluent = true)
@Value
class Frame implements IFrame {
    private final BufferedImage image;
    private final long nanostamp;
    private final IDecoderVideoStream stream;

    public String toString() {
    	return "Video frame t=" + nanostamp + " stream:" + stream.name();
    }
}

@Accessors(fluent = true)
@Value
@ToString
class MuxerProperties implements IMuxerProperties {
    private final String format;
}

@Accessors(fluent = true)
@Value
@ToString
class VideoStreamProperties implements IVideoStreamProperties {
    private final String codec;
    private final double framerate;
    private final long duration;
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
	private final IFrame frame;

	@Override
	public IFrame video() {
		return frame;
	}
	
	@Override
	public boolean isVideo() {
		return true;
	}
}
