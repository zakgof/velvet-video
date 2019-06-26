package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.zakgof.velvetvideo.FFMpegNative.AVCodec;
import com.zakgof.velvetvideo.FFMpegNative.AVCodecContext;
import com.zakgof.velvetvideo.FFMpegNative.AVFrame;
import com.zakgof.velvetvideo.FFMpegNative.AVPacket;
import com.zakgof.velvetvideo.FFMpegNative.AVPixelFormat;
import com.zakgof.velvetvideo.FFMpegNative.LibAVCodec;
import com.zakgof.velvetvideo.FFMpegNative.LibAVUtil;
import com.zakgof.velvetvideo.FFMpegNative.LibSwScale;
import com.zakgof.velvetvideo.FFMpegNative.SwsContext;
import com.zakgof.velvetvideo.IVideoLib.IEncoder.IBuilder;

import jnr.ffi.CallingConvention;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class FFMpegVideoLib implements IVideoLib {

    private Runtime runtime;

    @Override
    public IBuilder encoderBuilder(int width, int height, IEncoderStream output) {
        return new EncoderBuilderImpl(width, height, output);
    }

    private class EncoderBuilderImpl implements IBuilder {

        private final int width;
        private final int height;
        private final IEncoderStream output;
        private int timebaseNum = 1;
        private int timebaseDen = 30;
        private int bitrate = 400000;
        private Map<String, String> params = new HashMap<>();
        private String codec;

        public EncoderBuilderImpl(int width, int height, IEncoderStream output) {
            this.width = width;
            this.height = height;
            this.output = output;
        }

        @Override
        public IBuilder codec(String codec) {
            this.codec = codec;
            return this;
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
        public IEncoder build() {
            return new EncoderImpl(output, codec, width, height, bitrate, timebaseNum, timebaseDen, params);
        }

    }

    private class EncoderImpl implements IEncoder {

        private LibAVUtil libavutil;
        private LibAVCodec libavcodec;
        private AVPacket packet;
        private AVFrame frame;
        private AVCodecContext avcontext;
        private LibSwScale libswscale;
        private IEncoderStream output;
        

        private static final int AVERROR_EOF = -541478725;

        public EncoderImpl(IEncoderStream output, String codecName, int width, int height, int bitrate, int timebaseNum,
                           int timebaseDen, Map<String, String> params) {

            this.output = output;
            libavcodec = LibraryLoader.create(LibAVCodec.class).search("C:\\pr\\videojava\\src\\main\\resources\\").convention(CallingConvention.DEFAULT).load("avcodec-58");
            libavutil = LibraryLoader.create(LibAVUtil.class).search("C:\\pr\\videojava\\src\\main\\resources\\").load("avutil-56");
            libswscale = LibraryLoader.create(LibSwScale.class).search("C:\\pr\\videojava\\src\\main\\resources\\").load("swscale-5");

            
            
            AVCodec codec = libavcodec.avcodec_find_encoder_by_name(codecName);
            runtime = codec.getRuntime();
            avcontext = libavcodec.avcodec_alloc_context3(codec);

            avcontext.bit_rate.set(bitrate);
            avcontext.time_base.num.set(timebaseNum);
            avcontext.time_base.den.set(timebaseDen);
            avcontext.pix_fmt.set(0);// set(AVPixelFormat.AV_PIX_FMT_YUV420P);
            avcontext.width.set(width);
            avcontext.height.set(height);
            // avcontext.gop_size.set(10); /* emit one intra frame every ten frames */
            avcontext.max_b_frames.set(2);

            Pointer[] opts = new Pointer[1];
            for (Entry<String, String> entry : params.entrySet()) {
                libavutil.av_dict_set(opts, entry.getKey(), entry.getValue(), 0);
            }

            int res = libavcodec.avcodec_open2(avcontext, codec, opts);
            System.err.println("Open: " + res);

            frame = libavutil.av_frame_alloc();
            frame.width.set(width);
            frame.height.set(height);
            frame.pix_fmt.set(AVPixelFormat.AV_PIX_FMT_YUV420P);

            Pointer[] dataptr = new Pointer[8];
            int[] linesizeptr = new int[8];
            res = libavutil.av_image_alloc(dataptr, linesizeptr, frame.width.get(), frame.height.get(), frame.pix_fmt.get(), 32);

            for (int i = 0; i < 8; i++) {
                frame.data[i].set(dataptr[i]);
                frame.linesize[i].set(linesizeptr[i]);
            }

            packet = libavcodec.av_packet_alloc();
        }

        @Override
        public void encode(BufferedImage image, long pts) {

            AVPixelFormat srcFormat = avformatOf(image.getType());

            byte[] bytes = bytesOf(image);

            SwsContext ctx = libswscale.sws_getContext(image.getWidth(), image.getHeight(), srcFormat, image.getWidth(), image.getHeight(), AVPixelFormat.AV_PIX_FMT_YUV420P, 0, 0, 0, 0);

            int[] inStride = {3 * image.getWidth()}; // RGB stride
            int[] outStride = { image.getWidth(), image.getWidth() / 2, image.getWidth() / 2 }; // YUV

            Pointer imageptr = frame.getRuntime().getMemoryManager().allocateDirect(bytes.length);
            imageptr.put(0, bytes, 0, bytes.length);
            Pointer[] inData = new Pointer[] { imageptr }; // one plane
            Pointer[] outData = new Pointer[] { frame.data[0].get(), frame.data[1].get(), frame.data[2].get() };
            checkcode(libswscale.sws_scale(ctx, inData, inStride, 0, image.getHeight(), outData, outStride));

            System.err.println("Frame: " + pts);
            libavcodec.av_init_packet(packet);
            packet.data.set((Pointer) null); // packet data will be allocated by the encoder
            packet.size.set(0);

            frame.pts.set((int) pts);
            long address = Struct.getMemory(frame).address() + frame.data[0].offset();
            frame.extended_data.set(Pointer.wrap(frame.getRuntime(), address));
            encodeFrame(libavcodec, avcontext, frame, packet);

        }

        private void encodeFrame(LibAVCodec libavcodec, AVCodecContext avcontext, AVFrame frame, AVPacket packet) {
            int res;
            res = checkcode(libavcodec.avcodec_send_frame(avcontext, frame));
            System.err.println("Send frame " + res);
            if (res < 0)
                throw new RuntimeException("Error " + res);

            while (res >= 0) {
                res = libavcodec.avcodec_receive_packet(avcontext, packet);
                System.err.println("Recv packet " + res);
                if (res == -11 || res == AVERROR_EOF)
                    break;
                checkcode(res);
                System.err.println("OUTPUT " + packet.size.get() + " bytes");
                byte[] bts = new byte[packet.size.get()];
                packet.data.get().get(0, bts, 0, bts.length);
                output.send(bts);
            }

            libavcodec.av_free_packet(packet);
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
                Pointer ptr = runtime.getMemoryManager().allocate(256);
                libavutil.av_strerror(code, ptr, 256);
                byte[] bts = new byte[256];
                ptr.get(0, bts, 0, 256);
                int len = 0;
                for (int i=0; i<256; i++) if (bts[i] == 0) len = i;
                String s = new String(bts, 0, len);
                throw new RuntimeException("FFMPEG error " + code + " : "+ s);
            }
            return code;
        }

    }

    @Override
    public List<String> codecs() {
        // TODO Auto-generated method stub
        return null;
    }

}
