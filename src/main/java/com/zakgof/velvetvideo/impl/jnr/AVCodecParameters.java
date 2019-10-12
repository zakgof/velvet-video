package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.Struct.Pointer;
import jnr.ffi.Struct.Signed32;
import jnr.ffi.Struct.int32_t;
import jnr.ffi.Struct.int64_t;
import jnr.ffi.Struct.u_int64_t;

public class AVCodecParameters extends Struct {

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
}