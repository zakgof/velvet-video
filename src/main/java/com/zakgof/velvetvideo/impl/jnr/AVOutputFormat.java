package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class AVOutputFormat extends Struct {
    public AVOutputFormat(Runtime runtime) {
        super(runtime);
    }

    public Struct.String name = new AsciiStringRef();
    public Struct.String long_name = new AsciiStringRef();
    public Struct.String mime = new AsciiStringRef();
    public Struct.String extensions = new AsciiStringRef();

    Signed32 audio_codec = new Signed32();
    Signed32 video_codec = new Signed32();
    Signed32 subtitle_codec = new Signed32();

    public Signed32 flags = new Signed32();

}