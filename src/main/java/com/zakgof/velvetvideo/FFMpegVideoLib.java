package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import jnr.ffi.byref.PointerByReference;
import jnr.ffi.provider.ParameterFlags;
import jnr.ffi.provider.jffi.NativeRuntime;

public class FFMpegVideoLib implements IVideoLib {
    
    private static final int AVERROR_EOF = -541478725;
    private static final int AVERROR_EAGAIN = -11;

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
        libavutil = JNRLoader.load(LibAVUtil.class, "avutil-56");
        libswscale = JNRLoader.load(LibSwScale.class, "swscale-5");
        libavcodec = JNRLoader.load(LibAVCodec.class, "avcodec-58");
    }

    @Override
    public IBuilder encoderBuilder(String codec) {
        return new EncoderBuilderImpl(codec);
    }
    
    private AVCodecContext createCodecContext(EncoderBuilderImpl builder) {
        AVCodec codec = libavcodec.avcodec_find_encoder_by_name(builder.codec);
        AVCodecContext ctx = libavcodec.avcodec_alloc_context3(codec);
        ctx.bit_rate.set(builder.bitrate);
        ctx.time_base.num.set(builder.timebaseNum);
        ctx.time_base.den.set(builder.timebaseDen);
        ctx.pix_fmt.set(AVPixelFormat.AV_PIX_FMT_YUV420P); // TODO
        // avcontext.gop_size.set(10); /* emit one intra frame every ten frames */
        // avcontext.max_b_frames.set(2);            
        ctx.width.set(640); // TODO
        ctx.height.set(480);
        Pointer[] opts = createDictionary(builder);
        checkcode(libavcodec.avcodec_open2(ctx, codec, opts));
        return ctx;
    }

    private Pointer[] createDictionary(EncoderBuilderImpl builder) {
        Pointer[] opts = new Pointer[1];
        for (Entry<String, String> entry : builder.params.entrySet()) {
            libavutil.av_dict_set(opts, entry.getKey(), entry.getValue(), 0);
        }
        return opts;
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
        }       

        @Override
        public void encode(BufferedImage image, long pts) {

            int width = image.getWidth();
            int height = image.getHeight();
            codecCtx.width.set(width);
            codecCtx.height.set(height);

            if (frameHolder == null) {
                frameHolder = new FrameHolder(width, height, image.getType());
            }

            AVFrame frame = frameHolder.setPixels(image);
            frame.pts.set((int) pts);
            
            packet = libavcodec.av_packet_alloc();

            frame.extended_data.set(frame.data[0].getMemory());
            encodeFrame(frame, packet);
        }

        private void encodeFrame(AVFrame frame, AVPacket packet) {
            if (frame != null) {
                int cn = codecCtx.time_base.num.get();
                int cd = codecCtx.time_base.den.get();
                int newPts = (int) (frame.pts.get() * cn * containerTimeBaseDen / cd / containerTimeBaseNum);
                frame.pts.set(newPts);
            }
            checkcode(libavcodec.avcodec_send_frame(codecCtx, frame));
            for (;;) {
                libavcodec.av_init_packet(packet);
                packet.data.set((Pointer) null);
                packet.size.set(0);

                int res = libavcodec.avcodec_receive_packet(codecCtx, packet);
                System.err.println("Recv packet " + res);
                if (res == AVERROR_EAGAIN || res == AVERROR_EOF)
                    break;
                checkcode(res);
                byte[] data = new byte[packet.size.get()];
                packet.data.get().get(0, data, 0, data.length);
                System.err.println("OUTPUT " + packet.size.get() + " bytes, PTS=" + packet.pts.get() + " DTS=" + packet.dts.get());
                output.send(packet);
                libavcodec.av_packet_unref(packet);
                
            }
        
            // 
        }

        private class FrameHolder implements AutoCloseable {

            private final AVFrame frame;
            private final SwsContext scaleCtx;
            private final int width;
            private final int height;
            private final Pointer rgbPtr;

            public FrameHolder(int width, int height, int originalImageType) {
                this.width = width;
                this.height = height;

                this.frame = libavutil.av_frame_alloc();
                frame.width.set(width);
                frame.height.set(height);
                frame.pix_fmt.set(AVPixelFormat.AV_PIX_FMT_YUV420P); // TODO

                this.rgbPtr = frame.getRuntime().getMemoryManager().allocateDirect(width * height * 3);  // TODO

                AVPixelFormat srcFormat = avformatOf(originalImageType);
                scaleCtx = libswscale.sws_getContext(width, height, srcFormat, width, height, AVPixelFormat.AV_PIX_FMT_YUV420P, 0, 0, 0, 0);

                Pointer[] dataptr = new Pointer[8]; // TODO: this is ugly
                int[] linesizeptr = new int[8];
                checkcode(libavutil.av_image_alloc(dataptr, linesizeptr, width, height, AVPixelFormat.AV_PIX_FMT_YUV420P, 32));
                for (int i = 0; i < 8; i++) {
                    frame.data[i].set(dataptr[i]);
                    frame.linesize[i].set(linesizeptr[i]);
                }
            }

            public AVFrame setPixels(BufferedImage image) {
                byte[] bytes = bytesOf(image);
                rgbPtr.put(0, bytes, 0, bytes.length);

                int[] inStride = {3 * width}; // RGB stride
                int[] outStride = { width, width / 2, width / 2 }; // YUV

                Pointer[] inData = new Pointer[] { rgbPtr }; // one plane
                Pointer[] outData = new Pointer[] { frame.data[0].get(), frame.data[1].get(), frame.data[2].get() };
                checkcode(libswscale.sws_scale(scaleCtx, inData, inStride, 0, height, outData, outStride));
                return frame;
            }
            
            public void close() {
                // TODO: free frame and its buffers, scaleCtx
            }
        }

        private AVPixelFormat avformatOf(int type) {
            if (type == BufferedImage.TYPE_3BYTE_BGR) {
                return AVPixelFormat.AV_PIX_FMT_BGR24;
            } else {
                return null;
            }
        }

        private byte[] bytesOf(BufferedImage image) {
            Raster raster = image.getData();
            DataBuffer buffer = raster.getDataBuffer();
            if (buffer instanceof DataBufferByte) {
                return ((DataBufferByte) buffer).getData();
            }
            throw new VelvetVideoException("Unsupported image data buffer type");
        }

        @Override
        public void close() {
            // Flush
            encodeFrame(null, packet);
            output.close();
            System.err.println("ALL DONE !");
        }

    }

    @Override
    public List<String> codecs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMuxer.IBuilder muxerBuilder(String format) {
        return new MuxerBuilderImpl(format);
    }

    private class MuxerBuilderImpl implements IMuxer.IBuilder {

        private String format;
        private Map<String, IEncoder.IBuilder> videos = new LinkedHashMap<>();

        public MuxerBuilderImpl(String format) {
            this.format = format;
        }

        @Override
        public IMuxer.IBuilder videoStream(String name, IEncoder.IBuilder encoderBuilder) {
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
                FileOutput output = new FileOutput(new FileOutputStream(outputFile));
                return new MuxerImpl(format, output, videos);
            } catch (FileNotFoundException e) {
                throw new VelvetVideoException(e);
            }
        }
        
    }

    private class MuxerImpl implements IMuxer {

        private static final int AVFMT_FLAG_CUSTOM_IO =  0x0080; 
        private static final int AVFMT_GLOBALHEADER = 0x0040;
        private static final int CODEC_FLAG_GLOBAL_HEADER  = 1 << 22;
        
        private final LibAVFormat libavformat;
        private final Map<String, IEncoder> videoStreams = new LinkedHashMap<>();
        private final ISeekableOutput output;
        private AVFormatContext formatCtx;
        private AVIOContext avioCtx;

        public MuxerImpl(String format, ISeekableOutput output, Map<String, IEncoder.IBuilder> videoBuilders) {
            
            this.output = output;
            System.err.println("Muxing");
            this.libavformat = JNRLoader.load(LibAVFormat.class, "avformat-58");
            
            Pointer buffer = NativeRuntime.getInstance().getMemoryManager().allocateDirect(32768);
            
            IOCallback callback = new IOCallback();
            avioCtx = libavformat.avio_alloc_context(buffer, 32768, 1, null, null, callback, callback);
            
            AVOutputFormat outputFmt = libavformat.av_guess_format(format, null, null);

            PointerByReference ctxptr = new PointerByReference();
            checkcode(libavformat.avformat_alloc_output_context2(ctxptr, outputFmt, "mp4", null));
            
            formatCtx = new AVFormatContext(NativeRuntime.getInstance());
            formatCtx.useMemory(ctxptr.getValue());
            Struct.getMemory(formatCtx, ParameterFlags.OUT);

            //context.ctx_flags.get();
            //context.ctx_flags.set(AVFMT_FLAG_CUSTOM_IO);
            //context.pb.set(avioCtx);
            
            IPacketStream ps = new IPacketStream() {

                @Override
                public void send(AVPacket packet) {
                    int res = checkcode(libavformat.av_write_frame(formatCtx, packet));
                    System.err.println("Submit packet " + res);
                }

                @Override
                public void close() {
                    int res = checkcode(libavformat.av_write_frame(formatCtx, null));
                    System.err.println("Flush muxer " + res);
                }
                
            };
            
            Map<String, AVStream> streams = new HashMap<>();
            for (Entry<String, IEncoder.IBuilder> entry : videoBuilders.entrySet()) {
                
//                // TODO
//                // context.ctx_flags.set(context.ctx_flags.get() | CODEC_FLAG_GLOBAL_HEADER);
                
                EncoderBuilderImpl builder = (EncoderBuilderImpl)entry.getValue();
                AVCodec codec = libavcodec.avcodec_find_encoder_by_name(builder.codec);
                AVStream stream = libavformat.avformat_new_stream(formatCtx, codec);
                
                AVCodecContext codecContext = stream.codec.get();
                
                codecContext.codec_id.set(codec.id.get());
                codecContext.codec_type.set(codec.type.get());
                codecContext.bit_rate.set(builder.bitrate);
                codecContext.time_base.num.set(builder.timebaseNum);
                codecContext.time_base.den.set(builder.timebaseDen);
                codecContext.pix_fmt.set(AVPixelFormat.AV_PIX_FMT_YUV420P); // TODO
                // avcontext.gop_size.set(10); /* emit one intra frame every ten frames */
                // avcontext.max_b_frames.set(2);            
                codecContext.width.set(640); // TODO
                codecContext.height.set(480);
                codecContext.codec.set(codec);
                
                
                
                stream.id.set(formatCtx.nb_streams.get() - 1);
                // stream.codec.set(codecContext);
                
//                stream.codecpar.set(libavcodec.avcodec_parameters_alloc());
//                int rr = libavcodec.avcodec_parameters_from_context(stream.codecpar.get(), codecContext);
//                
                stream.time_base.den.set(builder.timebaseDen);
                stream.time_base.num.set(builder.timebaseNum);
                
                Pointer[] opts = createDictionary(builder);
                checkcode(libavcodec.avcodec_open2(codecContext, codec, opts));
                streams.put(entry.getKey(), stream);
            }
            
            // libavformat.av_dump_format(context, 0, "C:\\pr\\1.mp4", 1);
            
            PointerByReference pbref = new PointerByReference();
            libavformat.avio_open(pbref, "C:\\pr\\auto.mp4", 2);
            formatCtx.pb.set(pbref.getValue());
            
            checkcode(libavformat.avformat_write_header(formatCtx, null));
            
            
            for (Entry<String, AVStream> entry : streams.entrySet()) {                
                AVStream stream = entry.getValue();
                AVCodecContext codecCtx = stream.codec.get();
                videoStreams.put(entry.getKey(), new EncoderImpl(ps, codecCtx, stream.time_base.num.get(), stream.time_base.den.get()));
            }
            
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
        
        class IOCallback implements IPacketIO, ISeeker {

            @Override
            public int seek(Pointer opaque, int offset, int whence) {
                System.err.println("Seek custom avio to " + offset);
                output.seek(offset);
                return 0;
            }

            @Override
            public int read_packet(Pointer opaque, Pointer buf, int buf_size) {
                System.err.println("Writing to custom avio " + buf_size + " bytes");
                byte[] bytes = new byte[buf_size];
                buf.get(0, bytes, 0, buf_size);
                output.write(bytes);
                return 0;
            }
            
        }
        
    }
    
    
    
    interface IPacketStream extends AutoCloseable {
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
            byte[] bts = new byte[packet.size.get()];
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
    public IDecoder decoder(InputStream is) {
        // TODO Auto-generated method stub
        return null;
    }

}
