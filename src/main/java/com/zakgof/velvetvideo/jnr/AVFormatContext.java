package com.zakgof.velvetvideo.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class AVFormatContext extends Struct {

    public AVFormatContext(Runtime runtime) {
        super(runtime);
    }

    Pointer av_class = new Pointer();
    public StructRef<AVInputFormat> iformat = new StructRef<>(AVInputFormat.class);
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
    public Struct.StructRef<AVIOContext> pb = new StructRef<>(AVIOContext.class);
    public Signed32 ctx_flags = new Signed32();
    public Unsigned32 nb_streams = new Unsigned32();
    public Pointer streams = new Pointer();

    String filename = new Struct.UTF8String(1024);
    String url = new Struct.UTF8StringRef();
    int64_t start_time = new int64_t();
    public int64_t duration = new int64_t();
    int64_t bit_rate = new int64_t();
    Unsigned32 packet_size = new Unsigned32();
    Unsigned32 max_delay = new Unsigned32();
    Unsigned32 flags = new Unsigned32();
    int64_t probesize= new int64_t();
    int64_t max_analyze_duration = new int64_t();
    Pointer key = new Pointer();
    Signed32 keylen = new Signed32();
    Unsigned32 nb_programs = new Unsigned32();
    Pointer programs = new Pointer();
    public Signed32 video_codec_id = new Signed32();
    public Signed32 audio_codec_id = new Signed32();
    public Signed32 subtitle_codec_id = new Signed32();
    Unsigned32 max_index_size = new Unsigned32();
    Unsigned32 max_index_size2 = new Unsigned32();
    Unsigned32 nb_chapters = new Unsigned32();
    Pointer chapters = new Pointer();
    public Pointer metadata = new Pointer();
}