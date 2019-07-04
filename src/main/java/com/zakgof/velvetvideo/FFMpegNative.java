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
        int sws_scale(SwsContext ctx, Pointer inData, Pointer inStride, int srcSliceY, int height, Pointer outData, Pointer outStride);
        void sws_freeContext(SwsContext swsContext);
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
        
        int avcodec_receive_frame(AVCodecContext context, AVFrame frame);
        
        int avcodec_send_packet(AVCodecContext context, AVPacket packet);
        
        AVCodec avcodec_find_decoder(int id);
        
        AVCodec av_codec_iterate(PointerByReference opaque);
                                       

    }

    public interface LibAVUtil {
        AVFrame av_frame_alloc();
        int av_frame_get_buffer(AVFrame frame, int align);
        void av_frame_free(AVFrame[] frameref);

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
        
        int avformat_open_input(PointerByReference ctx, String url, AVInputFormat fmt, Pointer[] options);
        
        interface IPacketIO {
            @Delegate @StdCall int read_packet(Pointer opaque, Pointer buf, int buf_size);
        }

        interface ISeeker {
            @Delegate @StdCall int seek (Pointer opaque, int offset, int whence);
        }

        void av_dump_format(AVFormatContext context, int i, String string, int j);
        
        int avio_open(PointerByReference pbref, String url, int flags);
        
        AVFormatContext avformat_alloc_context();
        
        int avformat_find_stream_info(AVFormatContext context, Pointer[] options);
        
        int av_find_best_stream(AVFormatContext context, int type, int wanted_stream_nb, int related_stream,
                                Pointer[] decoder_ret,
                                int flags);
        
        int av_read_frame(AVFormatContext contexxt, AVPacket pkt);
        
        
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
        Signed32 capabilities = new Signed32();
        StructRef<AVRational> supported_framerates = new StructRef<>(AVRational.class); ///< array of supported framerates, or NULL if any, array is terminated by {0,0}
        Pointer pix_fmts = new Pointer();
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
        AV_PIX_FMT_BGR24, /// < packed RGB 8:8:8, 24bpp, BGRBGR...
        AV_PIX_FMT_YUV422P,   ///< planar YUV 4:2:2, 16bpp, (1 Cr & Cb sample per 2x1 Y samples)
        AV_PIX_FMT_YUV444P,   ///< planar YUV 4:4:4, 24bpp, (1 Cr & Cb sample per 1x1 Y samples)
        AV_PIX_FMT_YUV410P,   ///< planar YUV 4:1:0,  9bpp, (1 Cr & Cb sample per 4x4 Y samples)
        AV_PIX_FMT_YUV411P,   ///< planar YUV 4:1:1, 12bpp, (1 Cr & Cb sample per 4x1 Y samples)
        AV_PIX_FMT_GRAY8,     ///<        Y        ,  8bpp
        AV_PIX_FMT_MONOWHITE, ///<        Y        ,  1bpp, 0 is white, 1 is black, in each byte pixels are ordered from the msb to the lsb
        AV_PIX_FMT_MONOBLACK, ///<        Y        ,  1bpp, 0 is black, 1 is white, in each byte pixels are ordered from the msb to the lsb
        AV_PIX_FMT_PAL8,      ///< 8 bits with AV_PIX_FMT_RGB32 palette
        AV_PIX_FMT_YUVJ420P,  ///< planar YUV 4:2:0, 12bpp, full scale (JPEG), deprecated in favor of AV_PIX_FMT_YUV420P and setting color_range
        AV_PIX_FMT_YUVJ422P,  ///< planar YUV 4:2:2, 16bpp, full scale (JPEG), deprecated in favor of AV_PIX_FMT_YUV422P and setting color_range
        AV_PIX_FMT_YUVJ444P,  ///< planar YUV 4:4:4, 24bpp, full scale (JPEG), deprecated in favor of AV_PIX_FMT_YUV444P and setting color_range
        AV_PIX_FMT_UYVY422,   ///< packed YUV 4:2:2, 16bpp, Cb Y0 Cr Y1
        AV_PIX_FMT_UYYVYY411, ///< packed YUV 4:1:1, 12bpp, Cb Y0 Y1 Cr Y2 Y3
        AV_PIX_FMT_BGR8,      ///< packed RGB 3:3:2,  8bpp, (msb)2B 3G 3R(lsb)
        AV_PIX_FMT_BGR4,      ///< packed RGB 1:2:1 bitstream,  4bpp, (msb)1B 2G 1R(lsb), a byte contains two pixels, the first pixel in the byte is the one composed by the 4 msb bits
        AV_PIX_FMT_BGR4_BYTE, ///< packed RGB 1:2:1,  8bpp, (msb)1B 2G 1R(lsb)
        AV_PIX_FMT_RGB8,      ///< packed RGB 3:3:2,  8bpp, (msb)2R 3G 3B(lsb)
        AV_PIX_FMT_RGB4,      ///< packed RGB 1:2:1 bitstream,  4bpp, (msb)1R 2G 1B(lsb), a byte contains two pixels, the first pixel in the byte is the one composed by the 4 msb bits
        AV_PIX_FMT_RGB4_BYTE, ///< packed RGB 1:2:1,  8bpp, (msb)1R 2G 1B(lsb)
        AV_PIX_FMT_NV12,      ///< planar YUV 4:2:0, 12bpp, 1 plane for Y and 1 plane for the UV components, which are interleaved (first byte U and the following byte V)
        AV_PIX_FMT_NV21,      ///< as above, but U and V bytes are swapped

        AV_PIX_FMT_ARGB,      ///< packed ARGB 8:8:8:8, 32bpp, ARGBARGB...
        AV_PIX_FMT_RGBA,      ///< packed RGBA 8:8:8:8, 32bpp, RGBARGBA...
        AV_PIX_FMT_ABGR,      ///< packed ABGR 8:8:8:8, 32bpp, ABGRABGR...
        AV_PIX_FMT_BGRA,      ///< packed BGRA 8:8:8:8, 32bpp, BGRABGRA...

        AV_PIX_FMT_GRAY16BE,  ///<        Y        , 16bpp, big-endian
        AV_PIX_FMT_GRAY16LE,  ///<        Y        , 16bpp, little-endian
        AV_PIX_FMT_YUV440P,   ///< planar YUV 4:4:0 (1 Cr & Cb sample per 1x2 Y samples)
        AV_PIX_FMT_YUVJ440P,  ///< planar YUV 4:4:0 full scale (JPEG), deprecated in favor of AV_PIX_FMT_YUV440P and setting color_range
        AV_PIX_FMT_YUVA420P,  ///< planar YUV 4:2:0, 20bpp, (1 Cr & Cb sample per 2x2 Y & A samples)
        AV_PIX_FMT_RGB48BE,   ///< packed RGB 16:16:16, 48bpp, 16R, 16G, 16B, the 2-byte value for each R/G/B component is stored as big-endian
        AV_PIX_FMT_RGB48LE,   ///< packed RGB 16:16:16, 48bpp, 16R, 16G, 16B, the 2-byte value for each R/G/B component is stored as little-endian

        AV_PIX_FMT_RGB565BE,  ///< packed RGB 5:6:5, 16bpp, (msb)   5R 6G 5B(lsb), big-endian
        AV_PIX_FMT_RGB565LE,  ///< packed RGB 5:6:5, 16bpp, (msb)   5R 6G 5B(lsb), little-endian
        AV_PIX_FMT_RGB555BE,  ///< packed RGB 5:5:5, 16bpp, (msb)1X 5R 5G 5B(lsb), big-endian   , X=unused/undefined
        AV_PIX_FMT_RGB555LE,  ///< packed RGB 5:5:5, 16bpp, (msb)1X 5R 5G 5B(lsb), little-endian, X=unused/undefined

        AV_PIX_FMT_BGR565BE,  ///< packed BGR 5:6:5, 16bpp, (msb)   5B 6G 5R(lsb), big-endian
        AV_PIX_FMT_BGR565LE,  ///< packed BGR 5:6:5, 16bpp, (msb)   5B 6G 5R(lsb), little-endian
        AV_PIX_FMT_BGR555BE,  ///< packed BGR 5:5:5, 16bpp, (msb)1X 5B 5G 5R(lsb), big-endian   , X=unused/undefined
        AV_PIX_FMT_BGR555LE;  ///< packed BGR 5:5:5, 16bpp, (msb)1X 5B 5G 5R(lsb), little-endian, X=unused/undefined

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
        Pointer streams = new Pointer();

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
    
    public static class AVInputFormat extends Struct {
        public AVInputFormat(Runtime runtime) {
            super(runtime);
        }
        
        public Struct.String name = new AsciiStringRef();
        public Struct.String long_name = new AsciiStringRef();     
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
