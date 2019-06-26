package com.zakgof.velvetvideo;

import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.util.EnumMapper.IntegerEnum;

class FFMpegNative {

    public static interface LibSwScale {

        SwsContext sws_getContext(int width, int height, AVPixelFormat avPixFmtRgb24, int width2, int height2, AVPixelFormat avPixFmtYuv420p, int i, int j, int k, int l);

        int sws_scale(SwsContext ctx, Pointer[] inData, int[] inStride, int srcSliceY, int height, Pointer[] outData, int[] outStride);

    }

    public interface LibAVCodec {
        AVCodec avcodec_find_encoder_by_name(String name);

        AVPacket av_packet_alloc();

        int avcodec_receive_packet(AVCodecContext avcontext, AVPacket packet);

        int avcodec_open2x();

        void av_init_packet(@Out AVPacket packet);

        AVCodecContext avcodec_alloc_context3(AVCodec codec);

        // int avcodec_open2(AVCodecContext context, @In AVCodec codec, jnr.ffi.Pointer[] dict);
        int avcodec_open2(AVCodecContext context, @In AVCodec b, @In Pointer[] c);

        void avcodec_register_all();

        int avcodec_send_frame(AVCodecContext context, @In AVFrame frame);

        int avcodec_encode_video2(AVCodecContext context, AVPacket packet, @In AVFrame frame, @Out int[] got_output);

        void av_free_packet(AVPacket packet);

    }

    public interface LibAVUtil {
        AVFrame av_frame_alloc();

        int av_image_alloc(Pointer[] pointers, int[] linesizes, int w, int h, AVPixelFormat pix_fmt, int align);

        int av_dict_set(Pointer[] pm, String key, String value, int flags);

        int av_strerror(int errnum, Pointer errbuf, int errbuf_size);
    }

    public static class AVDictionary extends Struct {
        protected AVDictionary(Runtime runtime) {
            super(runtime);
        }
    }

    public static class AVPacket extends Struct {

        public AVPacket(Runtime runtime) {
            super(runtime);
        }

        Pointer buf = new Pointer();
        int64_t pts = new int64_t();
        int64_t dts = new int64_t();

        public Pointer data = new Pointer();
        public Signed32 size = new Signed32();
    }

    public static class AVCodecContext extends Struct {

        public AVCodecContext(Runtime runtime) {
            super(runtime);
        }

        // int64_t bit_rate = new int64_t();
        // AVRational time_base = new AVRational(getRuntime());
        // Signed32 width = new Signed32();
        // Signed32 height = new Signed32();
        // AVPixelFormat pix_fmt = AVPixelFormat.AV_PIX_FMT_YUV420P;

        Struct.Pointer av_class = new Pointer(); // const AVClass *av_class;
        Signed32 log_level_offset = new Signed32();
        Unsigned16 codec_type = new Unsigned16();
        Struct.Pointer codec = new Pointer();
        Unsigned8 codec_id = new Unsigned8();
        Unsigned32 codec_tag = new Unsigned32();
        Struct.Pointer priv_data = new Pointer();
        Struct.Pointer internal = new Pointer();
        Struct.Pointer opaque = new Pointer();
        int64_t bit_rate = new int64_t();
        Signed32 bit_rate_tolerance = new Signed32();
        Signed32 global_quality = new Signed32();
        Signed32 compression_level = new Signed32();
        Signed32 flags = new Signed32();
        Signed32 flags2 = new Signed32();
        Struct.Pointer extradata = new Pointer();
        Signed32 extradata_size = new Signed32();
        AVRational time_base = inner(new AVRational(getRuntime()));
        Signed32 ticks_per_frame = new Signed32();
        Signed32 delay = new Signed32();
        Signed32 width = new Signed32();
        Signed32 height = new Signed32();
        Signed32 coded_width = new Signed32();
        Signed32 coded_height = new Signed32();
        Signed32 gop_size = new Signed32();
        Enum32<AVPixelFormat> pix_fmt = new Enum32<>(AVPixelFormat.class);
        Signed32 max_b_frames = new Signed32();

        float b_quant_factor;
        float b_quant_offset;
        int has_b_frames;
        float i_quant_factor;
        float i_quant_offset;
        float lumi_masking;

        //
        // enum AVMediaType codec_type; /* see AVMEDIA_TYPE_xxx */
        // const struct AVCodec *codec;
        // enum AVCodecID codec_id; /* see AV_CODEC_ID_xxx */
        // unsigned int codec_tag;
        // void *priv_data;
        // struct AVCodecInternal *internal;
        // void *opaque;
        // int64_t bit_rate;
        // int bit_rate_tolerance;
        // int global_quality;
        // int compression_level;
        // int flags;
        // int flags2;
        // uint8_t *extradata;
        // int extradata_size;
        // AVRational time_base;
        // int ticks_per_frame;
        // int delay;
        // int width, height;
        // int coded_width, coded_height;
        // int gop_size;
        // enum AVPixelFormat pix_fmt;

    }

    public static class AVCodec extends Struct {

        public AVCodec(Runtime runtime) {
            super(runtime);
        }

        /**
         * Name of the codec implementation. The name is globally unique among encoders and among decoders (but an encoder and a decoder can share the same name). This is the primary way to find a codec from the user perspective.
         */

        public Struct.String name = new UTF8StringRef();
        /**
         * Descriptive name for the codec, meant to be more human readable than name.
         */
        public Struct.String long_name = new UTF8StringRef();
    }

    public static class AVRational extends Struct {

        public AVRational(Runtime runtime) {
            super(runtime);
        }

        public Signed32 num = new Signed32();
        public Signed32 den = new Signed32();
    }

    public static enum AVPixelFormat implements IntegerEnum {
        AV_PIX_FMT_YUV420P, /// < planar YUV 4:2:0, 12bpp, (1 Cr & Cb sample per 2x2 Y samples)
        AV_PIX_FMT_YUYV422, /// < packed YUV 4:2:2, 16bpp, Y0 Cb Y1 Cr
        AV_PIX_FMT_RGB24, /// < packed RGB 8:8:8, 24bpp, RGBRGB...
        AV_PIX_FMT_BGR24; /// < packed RGB 8:8:8, 24bpp, BGRBGR...

        public int intValue() {
            return ordinal();
        }
    }

    public static class AVFrame extends Struct {

        public AVFrame(Runtime runtime) {
            super(runtime);
        }

        public Pointer[] data = array(new Pointer[8]); // uint8_t *data[8];
        public Signed32[] linesize = array(new Signed32[8]);
        Pointer extended_data = new Pointer();

        Signed32 width = new Signed32();
        Signed32 height = new Signed32();
        Signed32 nb_samples = new Signed32();
        Enum32<AVPixelFormat> pix_fmt = new Enum32<>(AVPixelFormat.class);
        Signed32 key_frame = new Signed32();
        Signed32 AVPictureType = new Signed32();
        AVRational sample_aspect_ratio = inner(new AVRational(getRuntime()));

        int64_t pts = new int64_t();
    }

    public static class SwsContext extends Struct {

        public SwsContext(Runtime runtime) {
            super(runtime);
        }
    }

}
