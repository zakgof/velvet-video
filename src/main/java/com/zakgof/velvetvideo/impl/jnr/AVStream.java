package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class AVStream extends Struct {
    public AVStream(Runtime runtime) {
        super(runtime);
    }

    public Signed32 index = new Signed32();
    public Signed32 id = new Signed32();

    public StructRef<AVCodecContext> codec = new StructRef<>(AVCodecContext.class);

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

    public AVRational r_frame_rate = inner(new AVRational(getRuntime()));

    public Pointer recommended_encoder_configuration = new Pointer();
    public StructRef<AVCodecParameters> codecpar = new StructRef<>(AVCodecParameters.class);
    public Pointer info = new Pointer();

    public Signed32 pts_wrap_bits = new Signed32();
    public int64_t first_dts = new int64_t();
    public int64_t cur_dts = new int64_t();
    public int64_t last_IP_pts = new int64_t();

    public Signed32 last_IP_duration = new Signed32();
    public Signed32 probe_packets = new Signed32();
    public Signed32 codec_info_nb_frames = new Signed32();
    public Signed32 need_parsing = new Signed32(); // enum AVStreamParseType need_parsing; TODO
    public Pointer parser = new Pointer();
    public Pointer last_in_packet_buffer = new Pointer();


    // AVProbeData probe_data;
    public Pointer AVProbeData_filename = new Pointer();
    public Pointer AVProbeData_buf = new Pointer();
    public Signed32 AVProbeData_buf_size = new Signed32();
    public Pointer AVProbeData_mime_type = new Pointer();

}