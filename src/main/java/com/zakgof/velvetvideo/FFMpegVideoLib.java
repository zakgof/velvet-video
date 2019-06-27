package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.IOException;
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

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.byref.PointerByReference;
import jnr.ffi.provider.ParameterFlags;
import jnr.ffi.provider.jffi.NativeRuntime;

public class FFMpegVideoLib implements IVideoLib {

    private Runtime runtime;
    
    private LibAVUtil libavutil;
    private LibSwScale libswscale;
    private LibAVCodec libavcodec;
    
    
    public FFMpegVideoLib() {
        libavutil = LibraryLoader.create(LibAVUtil.class).search("C:\\pr\\velvet-video\\src\\main\\resources\\").load("avutil-56");
        libswscale = LibraryLoader.create(LibSwScale.class).search("C:\\pr\\velvet-video\\src\\main\\resources\\").load("swscale-5");
        libavcodec = LibraryLoader.create(LibAVCodec.class).search("C:\\pr\\velvet-video\\src\\main\\resources\\").load("avcodec-58");
    }

    @Override
    public IBuilder encoderBuilder(String codec) {
        return new EncoderBuilderImpl(codec);
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

        @Override
        public IEncoder build(OutputStream output) {
            return build(new OutputPacketStream(output));
        }
        
        private EncoderImpl build(IPacketStream stream) {
            return new EncoderImpl(stream, codec, bitrate, timebaseNum, timebaseDen, params);            
        }

    }

    private class EncoderImpl implements IEncoder {

        
        
        private AVPacket packet;
        private AVCodecContext avcontext;
        
        private IPacketStream output;
        

        private static final int AVERROR_EOF = -541478725;
        private static final int AVERROR_EAGAIN = -11;
        private FrameHolder frameHolder;
        private AVCodec codec;

        public EncoderImpl(IPacketStream output, String codecName, int bitrate, int timebaseNum,
                           int timebaseDen, Map<String, String> params) {

            this.output = output;
            
            

            codec = libavcodec.avcodec_find_encoder_by_name(codecName);
            runtime = codec.getRuntime();
            avcontext = libavcodec.avcodec_alloc_context3(codec);

            avcontext.bit_rate.set(bitrate);
            avcontext.time_base.num.set(timebaseNum);
            avcontext.time_base.den.set(timebaseDen);
            avcontext.pix_fmt.set(AVPixelFormat.AV_PIX_FMT_YUV420P); // TODO
            // avcontext.gop_size.set(10); /* emit one intra frame every ten frames */
            // avcontext.max_b_frames.set(2);
            
            avcontext.width.set(640);
            avcontext.height.set(480);

            Pointer[] opts = new Pointer[1];
            for (Entry<String, String> entry : params.entrySet()) {
                libavutil.av_dict_set(opts, entry.getKey(), entry.getValue(), 0);
            }

            checkcode(libavcodec.avcodec_open2(avcontext, codec, opts));
            

            packet = libavcodec.av_packet_alloc();
        }

        @Override
        public void encode(BufferedImage image, long pts) {

            int width = image.getWidth();
            int height = image.getHeight();
            avcontext.width.set(width);
            avcontext.height.set(height);

            if (frameHolder == null) {
                frameHolder = new FrameHolder(width, height, image.getType());
            }

            AVFrame frame = frameHolder.setPixels(image);
            frame.pts.set((int) pts);

            libavcodec.av_init_packet(packet);
            packet.data.set((Pointer) null);
            packet.size.set(0);

            frame.extended_data.set(frame.data[0].getMemory());
            encodeFrame(libavcodec, avcontext, frame, packet);
        }

        private void encodeFrame(LibAVCodec libavcodec, AVCodecContext avcontext, AVFrame frame, AVPacket packet) {
            int res;
            res = checkcode(libavcodec.avcodec_send_frame(avcontext, frame));
            System.err.println("Send frame " + res);
        
            while (res >= 0) {
                res = libavcodec.avcodec_receive_packet(avcontext, packet);
                System.err.println("Recv packet " + res);
                if (res == AVERROR_EAGAIN || res == AVERROR_EOF)
                    break;
                checkcode(res);
                System.err.println("OUTPUT " + packet.size.get() + " bytes");
                output.send(packet);
            }
        
            libavcodec.av_free_packet(packet);
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
            encodeFrame(libavcodec, avcontext, null, packet);
            output.close();
            System.err.println("ALL DONE !");

        }

        private int checkcode(int code) {
            if (code < 0) {
                Pointer ptr = runtime.getMemoryManager().allocate(256); // TODO !!!
                libavutil.av_strerror(code, ptr, 256);
                byte[] bts = new byte[256];
                ptr.get(0, bts, 0, 256);
                int len = 0;
                for (int i=0; i<256; i++) if (bts[i] == 0) len = i;
                String s = new String(bts, 0, len);
                throw new VelvetVideoException("FFMPEG error " + code + " : "+ s);
            }
            return code;
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
        public IMuxer build(OutputStream output) {
            return new MuxerImpl(format, output, videos);
        }
        
    }

    private class MuxerImpl implements IMuxer {

        private static final int AVFMT_FLAG_CUSTOM_IO =  0x0080; 
        private LibAVFormat libavformat;
        private Map<String, IEncoder> videoStreams = new LinkedHashMap<>();
        private OutputStream output;

        public MuxerImpl(String format, OutputStream output, Map<String, IEncoder.IBuilder> videoBuilders) {
            
            libavutil.av_log_set_level(40);
            
            this.output = output;
            System.err.println("Muxing");
            libavformat = LibraryLoader.create(LibAVFormat.class).search("C:\\pr\\velvet-video\\src\\main\\resources\\").load("avformat-58");
            
            Pointer buffer = NativeRuntime.getInstance().getMemoryManager().allocateDirect(32768);
            
            IOCallback callback = new IOCallback();
            // AVIOContext avioCtx = libavformat.avio_alloc_context(buffer, 32768, 1, null, callback, callback, callback);
            
            AVOutputFormat outputFmt = libavformat.av_guess_format(format, null, null);

            PointerByReference ctxptr = new PointerByReference();
            int ret = libavformat.avformat_alloc_output_context2(ctxptr, outputFmt, "mp4", "C:\\pr\\auto.mp4");
            
            AVFormatContext context = new AVFormatContext(NativeRuntime.getInstance());
            context.useMemory(ctxptr.getValue());
            Struct.getMemory(context, ParameterFlags.OUT);

//            context.ctx_flags.get();
//            context.ctx_flags.set(AVFMT_FLAG_CUSTOM_IO);
//            context.pb.set(avioCtx);
            
            AVOutputFormat format2 = context.oformat.get();
            
            
            for (Entry<String, IEncoder.IBuilder> entry : videoBuilders.entrySet()) {
                
                IPacketStream ps = new IPacketStream() {

                    @Override
                    public void send(AVPacket packet) {
                        int res = libavformat.av_write_frame(context, packet);
                        System.err.println("Submit packet " + res);
                    }

                    @Override
                    public void close() {
                        int res = libavformat.av_write_frame(context, null);
                        System.err.println("Flush muxer " + res);
                    }
                    
                };
                
                EncoderImpl encoder = ((EncoderBuilderImpl)entry.getValue()).build(ps);
                videoStreams.put(entry.getKey(), encoder);
                // TODO
                //if (oc->oformat->flags & AVFMT_GLOBALHEADER)
                //    c->flags |= CODEC_FLAG_GLOBAL_HEADER;
                AVStream stream = libavformat.avformat_new_stream(context, encoder.codec);
                
                Pointer pars = libavcodec.avcodec_parameters_alloc();
                int rr = libavcodec.avcodec_parameters_from_context(pars, encoder.avcontext);
                
                AVCodecContext avCodecContext = stream.codec.get();
                libavcodec.avcodec_parameters_to_context(avCodecContext, pars);
                
                
                
//                Pointer pointer = stream.codecpar.get();
//                stream.codecpar.set(pars);
                
                // System.err.println(stream);
            }
            
            libavformat.av_dump_format(context, 0, "C:\\pr\\auto.mp4", 1);
            
            PointerByReference pbref = new PointerByReference();
            ret = libavformat.avio_open(pbref, "C:\\pr\\auto.mp4", 2);
            context.pb.set(pbref.getValue());
            
            
            
            ret = libavformat.avformat_write_header(context, null);
            
            
        }

        @Override
        public void close() {
            for (IEncoder encoder : videoStreams.values()) {
                encoder.close();
            }
            try {
                output.close();
            } catch (IOException e) {
                throw new VelvetVideoException(e);
            }
        }

        @Override
        public IEncoder videoStream(String name) {
            return videoStreams.get(name);
        }
        
        class IOCallback implements IPacketIO, ISeeker {

            @Override
            public int seek(Pointer opaque, int offset, int whence) {
                return 0;
            }

            @Override
            public int read_packet(Pointer opaque, Pointer buf, int buf_size) {
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

}
