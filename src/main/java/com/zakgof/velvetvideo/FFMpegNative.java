package com.zakgof.velvetvideo;

import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.annotations.Delegate;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.annotations.StdCall;
import jnr.ffi.byref.PointerByReference;
import jnr.ffi.types.size_t;
import jnr.ffi.util.EnumMapper.IntegerEnum;

class FFMpegNative {

    public static interface LibSwScale {

        SwsContext sws_getContext(int width, int height, AVPixelFormat avPixFmtRgb24, int width2, int height2, AVPixelFormat avPixFmtYuv420p, int i, int j, int k, int l);

        int sws_scale(SwsContext ctx, Pointer[] inData, int[] inStride, int srcSliceY, int height, Pointer[] outData, int[] outStride);

    }

    public interface LibAVCodec {
        AVCodec avcodec_find_encoder_by_name(String name);

        AVPacket av_packet_alloc();
        void av_init_packet(AVPacket packet);
        void av_packet_free(PointerByReference packet);
        void av_packet_unref(AVPacket packet);

        int avcodec_receive_packet(AVCodecContext avcontext, AVPacket packet);
        int avcodec_open2x();        

        AVCodecContext avcodec_alloc_context3(AVCodec codec);

        // int avcodec_open2(AVCodecContext context, @In AVCodec codec, jnr.ffi.Pointer[] dict);
        int avcodec_open2(AVCodecContext context, @In AVCodec b, @In Pointer[] c);

        void avcodec_register_all();

        int avcodec_send_frame(AVCodecContext context, @In AVFrame frame);

        int avcodec_encode_video2(AVCodecContext context, AVPacket packet, @In AVFrame frame, @Out int[] got_output);

        // void av_free_packet(AVPacket packet);
        
        
        int avcodec_parameters_from_context(Pointer par, @In AVCodecContext codec);
        
        int avcodec_parameters_to_context(@Out AVCodecContext codec, Pointer par);
        
        Pointer avcodec_parameters_alloc();
        
        void avcodec_parameters_free(PointerByReference par);
                                       

    }

    public interface LibAVUtil {
        AVFrame av_frame_alloc();

        int av_image_alloc(Pointer[] pointers, int[] linesizes, int w, int h, AVPixelFormat pix_fmt, int align);

        int av_dict_set(Pointer[] pm, String key, String value, int flags);

        int av_strerror(int errnum, Pointer errbuf, int errbuf_size);
        
        int av_log_set_level(int val);
        
        Pointer av_malloc(@size_t int size);
    }

    public interface LibAVFormat {
        int avformat_alloc_output_context2(@Out PointerByReference ctx, AVOutputFormat oformat, String format_name, String filename);
        AVStream avformat_new_stream(AVFormatContext ctx, @In AVCodec codec);
        int avformat_write_header(AVFormatContext ctx, Pointer[] dictionary);
        int av_write_trailer(AVFormatContext ctx);
        void avformat_free_context(AVFormatContext ctx);
        
        AVOutputFormat av_guess_format(String short_name, String filename, String mime_type);
        
        int av_write_frame(AVFormatContext context, AVPacket packet);
        int av_interleaved_write_frame(AVFormatContext ctx, AVPacket packet);
        
        AVIOContext avio_alloc_context(Pointer buffer, int buffer_size, int write_flag, Pointer opaque,
                                       IPacketIO reader,
                                       IPacketIO writer,
                                       ISeeker seeker);
        
        void avio_context_free(PointerByReference avioContext);
        
        interface IPacketIO {
            @Delegate @StdCall int read_packet(Pointer opaque, Pointer buf, int buf_size);
        }

        interface ISeeker {
            @Delegate @StdCall int seek (Pointer opaque, int offset, int whence);
        }

        void av_dump_format(AVFormatContext context, int i, String string, int j);
        
        int avio_open(PointerByReference pbref, String url, int flags);
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

        public Pointer buf = new Pointer();
        public int64_t pts = new int64_t();
        public int64_t dts = new int64_t();

        public Pointer data = new Pointer();
        public Signed32 size = new Signed32();
        public Signed32 stream_index = new Signed32();
    }

    public static class AVCodecContext extends Struct {

        public AVCodecContext(Runtime runtime) {
            super(runtime);
        }

        public Struct.Pointer av_class = new Pointer(); // const AVClass *av_class;
        public Signed32 log_level_offset = new Signed32();
        public Signed32 codec_type = new Signed32();
        public StructRef<AVCodec> codec = new StructRef<>(AVCodec.class);
        public Signed32 codec_id = new Signed32();
        public Unsigned32 codec_tag = new Unsigned32();
        public Struct.Pointer priv_data = new Pointer();
        public Struct.Pointer internal = new Pointer();
        public Struct.Pointer opaque = new Pointer();
        public int64_t bit_rate = new int64_t();
        public Signed32 bit_rate_tolerance = new Signed32();
        public Signed32 global_quality = new Signed32();
        public Signed32 compression_level = new Signed32();
        public Signed32 flags = new Signed32();
        public Signed32 flags2 = new Signed32();
        public Struct.Pointer extradata = new Pointer();
        public Signed32 extradata_size = new Signed32();
        public AVRational time_base = inner(new AVRational(getRuntime()));
        public Signed32 ticks_per_frame = new Signed32();
        public Signed32 delay = new Signed32();
        public Signed32 width = new Signed32();
        public Signed32 height = new Signed32();
        public Signed32 coded_width = new Signed32();
        public Signed32 coded_height = new Signed32();
        public Signed32 gop_size = new Signed32();
        public Enum32<AVPixelFormat> pix_fmt = new Enum32<>(AVPixelFormat.class);
        public Signed32 max_b_frames = new Signed32();

        public float b_quant_factor;
        public float b_quant_offset;
        public int has_b_frames;
        public float i_quant_factor;
        public float i_quant_offset;
        public float lumi_masking;

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
    
    public static class AVCodecParameters extends Struct {

        public AVCodecParameters(Runtime runtime) {
            super(runtime);
        }

        public Signed32 codec_type = new Signed32();
        public Signed32 codec_id = new Signed32();
        public int32_t codec_tag = new int32_t();
        
        public Struct.Pointer extradata = new Pointer();
        public Signed32 extradata_size = new Signed32();
        public Signed32 format = new Signed32();
        public int64_t bit_rate = new int64_t();
        public Signed32 bits_per_coded_sample = new Signed32();
        public Signed32 bits_per_raw_sample = new Signed32();
        
        public Signed32 profile = new Signed32();
        public Signed32 level = new Signed32();
        public Signed32 width = new Signed32();
        public Signed32 height = new Signed32();
        
        AVRational sample_aspect_ratio = inner(new AVRational(getRuntime()));
        Signed32 field_order = new Signed32();
        Signed32 color_range = new Signed32();
        Signed32 color_primaries = new Signed32();
        Signed32 color_trc = new Signed32();
        Signed32 color_space = new Signed32();
        Signed32 chroma_location = new Signed32();

        Signed32 video_delay = new Signed32();
        u_int64_t channel_layout = new u_int64_t();
        Signed32 channels = new Signed32();
        Signed32 sample_rate = new Signed32();
    };

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
        
        Signed32 type = new Signed32();
        Signed32 id = new Signed32();
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
    
    public static class AVFormatContext extends Struct {

        public AVFormatContext(Runtime runtime) {
            super(runtime);
        }
        
        Pointer av_class = new Pointer();
        Pointer iformat = new Pointer();
        StructRef<AVOutputFormat> oformat = new StructRef<>(AVOutputFormat.class);
        Pointer priv_data = new Pointer();

        /**
         * I/O context.
         *
         * - demuxing: either set by the user before avformat_open_input() (then
         *             the user must close it manually) or set by avformat_open_input().
         * - muxing: set by the user before avformat_write_header(). The caller must
         *           take care of closing / freeing the IO context.
         *
         * Do NOT set this field if AVFMT_NOFILE flag is set in
         * iformat/oformat.flags. In such a case, the (de)muxer will handle
         * I/O in some other way and this field will be NULL.
         */
        Struct.StructRef<AVIOContext> pb = new StructRef<>(AVIOContext.class);
        Signed32 ctx_flags = new Signed32();
        Unsigned32 nb_streams = new Unsigned32();

    }

    public static class AVOutputFormat extends Struct {
        public AVOutputFormat(Runtime runtime) {
            super(runtime);
        }
        
        public Struct.String name = new AsciiStringRef();
        public Struct.String long_name = new AsciiStringRef();
        public Struct.String mime = new AsciiStringRef();
        public Struct.String extensions = new AsciiStringRef();
     
    }
    
    public static class AVIOContext extends Struct {
        public AVIOContext(Runtime runtime) {
            super(runtime);
        }
    }

    public static class AVStream extends Struct {
        public AVStream(Runtime runtime) {
            super(runtime);
        }
        
        public Signed32 index = new Signed32();
        public Signed32 id = new Signed32();
        
        public StructRef<AVCodecContext> codec = new StructRef<AVCodecContext>(AVCodecContext.class);
      
        public Pointer priv_data = new Pointer();
        public AVRational time_base = inner(new AVRational(getRuntime()));

        public int64_t start_time = new int64_t();
        public int64_t duration = new int64_t();
        public int64_t nb_frames = new int64_t();

        public int32_t disposition = new int32_t();

        public int32_t discard = new int32_t();


        public AVRational sample_aspect_ratio = inner(new AVRational(getRuntime()));

        public Pointer metadata = new Pointer();

        public AVRational avg_frame_rate = inner(new AVRational(getRuntime()));

        public AVPacket attached_pic = inner(new AVPacket(getRuntime()));

        public Pointer side_data = new Pointer();
        public int32_t            nb_side_data = new int32_t();

        public int32_t event_flags = new int32_t();

        public  AVRational r_frame_rate = inner(new AVRational(getRuntime()));

       public Pointer codecpar = new Pointer();
    }

}
