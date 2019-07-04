package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.zakgof.velvetvideo.FFMpegNative.AVCodec;
import com.zakgof.velvetvideo.FFMpegNative.AVCodecContext;
import com.zakgof.velvetvideo.FFMpegNative.AVFormatContext;
import com.zakgof.velvetvideo.FFMpegNative.AVFrame;
import com.zakgof.velvetvideo.FFMpegNative.AVIOContext;
import com.zakgof.velvetvideo.FFMpegNative.AVOutputFormat;
import com.zakgof.velvetvideo.FFMpegNative.AVPacket;
import com.zakgof.velvetvideo.FFMpegNative.AVPixelFormat;
import com.zakgof.velvetvideo.FFMpegNative.AVStream;
import com.zakgof.velvetvideo.FFMpegNative.LibAVCodec;
import com.zakgof.velvetvideo.FFMpegNative.LibAVFormat;
import com.zakgof.velvetvideo.FFMpegNative.LibAVFormat.IPacketIO;
import com.zakgof.velvetvideo.FFMpegNative.LibAVFormat.ISeeker;
import com.zakgof.velvetvideo.FFMpegNative.LibAVUtil;
import com.zakgof.velvetvideo.FFMpegNative.LibSwScale;
import com.zakgof.velvetvideo.FFMpegNative.SwsContext;
import com.zakgof.velvetvideo.IVideoLib.IEncoder.IBuilder;

import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.Struct.int64_t;
import jnr.ffi.byref.PointerByReference;

public class FFMpegVideoLib implements IVideoLib {
    
    private static final int AVIO_CUSTOM_BUFFER_SIZE = 32768;
    private static final int AVFMT_FLAG_CUSTOM_IO =  0x0080; 
    private static final int AVFMT_GLOBALHEADER = 0x0040;
    private static final int CODEC_FLAG_GLOBAL_HEADER  = 1 << 22;
    
    private static final int AVMEDIA_TYPE_VIDEO = 0;
    private static final int AVMEDIA_TYPE_AUDIO = 1;
    
    private static final int AVERROR_EOF = -541478725;
    private static final int AVERROR_EAGAIN = -11;
    private static final long AVNOPTS_VALUE = -9223372036854775808L;

    private final Runtime runtime = Runtime.getSystemRuntime();
    
    private final LibAVUtil libavutil;
    private final LibSwScale libswscale;
    private final LibAVCodec libavcodec;
    
    private int checkcode(int code) {
        if (code < 0) {
            Pointer ptr = runtime.getMemoryManager().allocateDirect(512); // TODO !!!
            libavutil.av_strerror(code, ptr, 512);
            byte[] bts = new byte[512];
            ptr.get(0, bts, 0, 512);
            int len = 0;
            for (int i=0; i<512; i++) if (bts[i] == 0) len = i;
            String s = new String(bts, 0, len);
            throw new VelvetVideoException("FFMPEG error " + code + " : "+ s);
        }
        return code;
    }

    public FFMpegVideoLib() {
        libavutil = JNRHelper.load(LibAVUtil.class, "avutil-56");
        libswscale = JNRHelper.load(LibSwScale.class, "swscale-5");
        libavcodec = JNRHelper.load(LibAVCodec.class, "avcodec-58");
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
        ctx.pix_fmt.set(firstFormat);
        System.err.println("FORMAT " + ctx.pix_fmt.get());
        // avcontext.gop_size.set(10); /* emit one intra frame every ten frames */
        // avcontext.max_b_frames.set(2);            
        ctx.width.set(640); // TODO
        ctx.height.set(480);
        Pointer[] opts = createDictionary(builder);
        checkcode(libavcodec.avcodec_open2(ctx, codec, opts));
        
    }

    private Pointer[] createDictionary(EncoderBuilderImpl builder) {
        Pointer[] opts = new Pointer[1];
        for (Entry<String, String> entry : builder.params.entrySet()) {
            libavutil.av_dict_set(opts, entry.getKey(), entry.getValue(), 0);
        }
        return opts;
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

    private class EncoderBuilderImpl implements IBuilder {

        private final String codec;
        private int timebaseNum = 1;
        private int timebaseDen = 30;
        private int bitrate = 400000;
        private Map<String, String> params = new HashMap<>();

        public EncoderBuilderImpl(String codec) {
            this.codec = codec;
        }

        @Override
        public IBuilder framerate(int framerate) {
            this.timebaseNum = framerate;
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

        private EncoderImpl build(IPacketStream stream) {
            return new EncoderImpl(stream, this, timebaseNum, timebaseDen);      
        }

        @Override
        public IEncoder build(OutputStream output) {
            return build(new OutputPacketStream(output));
        }

    }

    private class EncoderImpl implements IEncoder {
        
        private AVPacket packet;
        private AVCodecContext codecCtx;
        
        private IPacketStream output;

        private FrameHolder frameHolder;
        private int containerTimeBaseNum;
        private int containerTimeBaseDen;

        private EncoderImpl(IPacketStream output, EncoderBuilderImpl builder, int containerTimeBaseNum, int containerTimeBaseDen) {            
            this(output, createCodecContext(builder), containerTimeBaseNum, containerTimeBaseDen);
        }
        
        private EncoderImpl(IPacketStream output, AVCodecContext codecCtx, int containerTimeBaseNum, int containerTimeBaseDen) {
            this.output = output;            
            this.codecCtx = codecCtx;
            this.containerTimeBaseNum = containerTimeBaseNum;
            this.containerTimeBaseDen = containerTimeBaseDen;
            this.packet = libavcodec.av_packet_alloc(); // TODO free
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
            checkcode(libavcodec.avcodec_send_frame(codecCtx, frame));
            for (;;) {
                libavcodec.av_init_packet(packet);
                packet.data.set((Pointer) null);
                packet.size.set(0);

                int res = libavcodec.avcodec_receive_packet(codecCtx, packet);
                if (res == AVERROR_EAGAIN || res == AVERROR_EOF)
                    break;
                checkcode(res);
                fixPacketPtsDts(packet);
                output.send(packet);
                libavcodec.av_packet_unref(packet);
            }
        
            // 
        }

        private void fixPacketPtsDts(AVPacket packet) {
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

        @Override
        public void close() {
            // Flush
            encodeFrame(null, packet);
            output.close();
        }

    }

    @Override
    public List<String> codecs() {
        PointerByReference ptr = new PointerByReference();
        AVCodec codec;
        List<String> codecs = new ArrayList<>();
        while ((codec = libavcodec.av_codec_iterate(ptr)) != null) {
            codecs.add(codec.name.get());
        }
        return codecs;
    }

    @Override
    public IMuxer.IBuilder muxer(String format) {
        return new MuxerBuilderImpl(format);
    }

    private class MuxerBuilderImpl implements IMuxer.IBuilder {

        private String format;
        private Map<String, IEncoder.IBuilder> videos = new LinkedHashMap<>();

        public MuxerBuilderImpl(String format) {
            this.format = format;
        }

        @Override
        public IMuxer.IBuilder video(String name, IEncoder.IBuilder encoderBuilder) {
            videos.put(name, encoderBuilder);
            return this;
        }

        @Override
        public IMuxer build(ISeekableOutput output) {
            return new MuxerImpl(format, output, videos);
        }

        @Override
        public IMuxer build(File outputFile) {
            try {
                FileSeekableOutput output = new FileSeekableOutput(new FileOutputStream(outputFile));
                return new MuxerImpl(format, output, videos);
            } catch (FileNotFoundException e) {
                throw new VelvetVideoException(e);
            }
        }
        
    }

    private class MuxerImpl implements IMuxer {

        
        
        private final LibAVFormat libavformat;
        private final Map<String, IEncoder> videoStreams;
        private final ISeekableOutput output;
        private AVFormatContext formatCtx;
        private AVIOContext avioCtx;
        private IOCallback callback;

        public MuxerImpl(String format, ISeekableOutput output, Map<String, IEncoder.IBuilder> videoBuilders) {
            
            this.output = output;
            this.libavformat = JNRHelper.load(LibAVFormat.class, "avformat-58");
            
            AVOutputFormat outputFmt = libavformat.av_guess_format(format, null, null);
            if (outputFmt == null) {
                throw new VelvetVideoException("Unsupported format: " + format);
            }

            PointerByReference ctxptr = new PointerByReference();
            checkcode(libavformat.avformat_alloc_output_context2(ctxptr, outputFmt, null, null));
            this.formatCtx = JNRHelper.struct(AVFormatContext.class, ctxptr.getValue()); 

            this.callback = new IOCallback();
            Pointer buffer = libavutil.av_malloc(AVIO_CUSTOM_BUFFER_SIZE + 64); // TODO free buffer
            avioCtx = libavformat.avio_alloc_context(buffer, AVIO_CUSTOM_BUFFER_SIZE, 1, null, null, callback, callback);
            int flagz = formatCtx.ctx_flags.get();
            formatCtx.ctx_flags.set(AVFMT_FLAG_CUSTOM_IO | flagz);
            formatCtx.pb.set(avioCtx);
            
            IPacketStream ps = new IPacketStream() {

                @Override
                public void send(AVPacket packet) {
                    int res = checkcode(libavformat.av_write_frame(formatCtx, packet));
                }

                @Override
                public void close() {
                    int res = checkcode(libavformat.av_write_frame(formatCtx, null));
                }
                
            };
            
            Map<String, AVStream> streams = videoBuilders.entrySet().stream()
               .collect(Collectors.toMap(Entry::getKey, entry -> createVideoStream((EncoderBuilderImpl)entry.getValue())));
         
            checkcode(libavformat.avformat_write_header(formatCtx, null));
            
            this.videoStreams = streams.entrySet().stream()
               .collect(Collectors.toMap(Entry::getKey, entry -> {
                   AVStream stream = entry.getValue();
                   AVCodecContext codecCtx = stream.codec.get();
                   return new EncoderImpl(ps, codecCtx, stream.time_base.num.get(), stream.time_base.den.get());
               }));
            
        }

        private AVStream createVideoStream(EncoderBuilderImpl builder) {
            AVCodec codec = libavcodec.avcodec_find_encoder_by_name(builder.codec);
            AVStream stream = libavformat.avformat_new_stream(formatCtx, codec);
            
            AVCodecContext codecContext = stream.codec.get();
            if ((formatCtx.ctx_flags.get() & AVFMT_GLOBALHEADER) != 0) {
                codecContext.flags.set(codecContext.flags.get() | CODEC_FLAG_GLOBAL_HEADER);
            }
            configureCodecContext(codecContext, codec, builder);
                            
            stream.id.set(formatCtx.nb_streams.get() - 1);
            stream.time_base.den.set(builder.timebaseDen);
            stream.time_base.num.set(builder.timebaseNum);
            return stream;
        }

        @Override
        public void close() {
            for (IEncoder encoder : videoStreams.values()) {
                encoder.close();
            }
            checkcode(libavformat.av_write_trailer(formatCtx));
            libavformat.avio_context_free(new PointerByReference(Struct.getMemory(avioCtx)));
            libavformat.avformat_free_context(formatCtx);
            output.close();
        }

        @Override
        public IEncoder videoStream(String name) {
            return videoStreams.get(name);
        }
        
        public class IOCallback implements IPacketIO, ISeeker {

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
        
    }
    
    private interface IPacketStream extends AutoCloseable {
        void send(AVPacket packet);
        void close();
    }
    
    private class OutputPacketStream implements IPacketStream {

        private final OutputStream output;

        public OutputPacketStream(OutputStream output) {
            this.output = output;
        }

        @Override
        public void send(AVPacket packet) {
            byte[] bts = new byte[packet.size.get()]; // TODO perf: preallocate buffer
            packet.data.get().get(0, bts, 0, bts.length);
            try {
                output.write(bts);
            } catch (IOException e) {
                throw new VelvetVideoException(e);
            }
        }

        @Override
        public void close() {
            try {
                output.close();
            } catch (IOException e) {
                throw new VelvetVideoException(e);
            }
        }
    }

    @Override
    public IDemuxer demuxer(InputStream is) {
        return new DemuxerImpl((FileInputStream) is);
    }
    
    private class DemuxerImpl implements IDemuxer {
        
        private final LibAVFormat libavformat;
        private AVFormatContext formatCtx;
        private ISeekableInput input;
        private IOCallback callback;
        private AVIOContext avioCtx;
        private List<IDecoderVideoStream> streams = new ArrayList<>();

        public DemuxerImpl(FileInputStream input) {
            this.input = new FileSeekableInput(input);
            this.libavformat = JNRHelper.load(LibAVFormat.class, "avformat-58");
            
            formatCtx = libavformat.avformat_alloc_context();
            this.callback = new IOCallback();
            Pointer buffer = libavutil.av_malloc(AVIO_CUSTOM_BUFFER_SIZE + 64); // TODO free buffer
            avioCtx = libavformat.avio_alloc_context(buffer, AVIO_CUSTOM_BUFFER_SIZE, 0, null, callback, null, callback);
            int flagz = formatCtx.ctx_flags.get();
            formatCtx.ctx_flags.set(AVFMT_FLAG_CUSTOM_IO | flagz);
            formatCtx.pb.set(avioCtx);
            
            PointerByReference ptrctx = new PointerByReference(Struct.getMemory(formatCtx));
            checkcode(libavformat.avformat_open_input(ptrctx, null, null, null));
            
            checkcode(libavformat.avformat_find_stream_info(formatCtx, null));
            
            long nb = formatCtx.nb_streams.get();
            for (long i=0; i<nb; i++) {
                AVStream avstream = JNRHelper.struct(AVStream.class, formatCtx.streams.get().getPointer(i /** TODO **/));
                if (avstream.codec.get().codec_type.get() == AVMEDIA_TYPE_VIDEO) {
                    streams.add(new DecoderVideoStreamImpl(avstream));
                }
            }
            toString();
        }
        
        private class DecoderVideoStreamImpl implements IDecoderVideoStream {

            private AVStream avstream;
            private AVCodecContext codecCtx;
            private AVPacket packet;
            private FrameHolder frameHolder;

            public DecoderVideoStreamImpl(AVStream avstream) {
                this.avstream = avstream;
                this.codecCtx = avstream.codec.get();
                AVCodec codec = libavcodec.avcodec_find_decoder(codecCtx.codec_id.get());
                checkcode(libavcodec.avcodec_open2(codecCtx, codec, null));
                this.packet = libavcodec.av_packet_alloc(); // TODO free
            }

            @Override
            public void close() {
                // TODO Auto-generated method stub
                
            }

            @Override
            public BufferedImage nextFrame() {
                // TODO: wrong API - should read audio or video or whatever comes

                libavcodec.av_init_packet(packet);
                packet.data.set((Pointer) null);
                packet.size.set(0);
                
                int res;
                do {
                    AVPacket p = packet;
                    res = libavformat.av_read_frame(formatCtx, packet);
                    if (res == AVERROR_EOF || res == -1) {
                        p = null;
                    } else {
                        checkcode(res);
                    }
                    res = decodePacket(p);
                } while (res == AVERROR_EAGAIN);
                if (res == AVERROR_EOF)
                    return null;
                checkcode(res);
                return frameHolder.getPixels();
            }
            
            private int decodePacket(AVPacket pack) {
                int res = libavcodec.avcodec_send_packet(codecCtx, pack);
                if (res != AVERROR_EOF)
                    checkcode(res);
                if (frameHolder == null) {
                    this.frameHolder = new FrameHolder(codecCtx.width.get(), codecCtx.height.get(), codecCtx.pix_fmt.get(), AVPixelFormat.AV_PIX_FMT_BGR24, false);
                }
                return libavcodec.avcodec_receive_frame(codecCtx, frameHolder.frame);
            }
            
        }
        
        @Override
        public List<IDecoderVideoStream> videoStreams() {
            return streams;
        }

        @Override
        public void close() {
            // TODO Auto-generated method stub
            
        }
        
        public class IOCallback implements IPacketIO, ISeeker {

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
                
                
                // System.err.println("Seek custom avio to " + offset + "/" + whence); // TODO whence
                // output.seek(offset);
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

      
        
    }

}
