package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class AVFrame extends Struct {

    public AVFrame(Runtime runtime) {
        super(runtime);
    }

    public Pointer[] data = array(new Pointer[8]); // uint8_t *data[8];
    public Signed32[] linesize = array(new Signed32[8]);
    public Pointer extended_data = new Pointer();

    public Signed32 width = new Signed32();
    public Signed32 height = new Signed32();
    public Signed32 nb_samples = new Signed32();
    public Enum32<AVPixelFormat> pix_fmt = new Enum32<>(AVPixelFormat.class);
    public Signed32 key_frame = new Signed32();
    public Signed32 AVPictureType = new Signed32();
    public AVRational sample_aspect_ratio = inner(new AVRational(getRuntime()));

    public int64_t pts = new int64_t();
    int64_t pkt_pts = new int64_t();
    int64_t pkt_dts = new int64_t();
}