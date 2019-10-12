package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.Struct.AsciiStringRef;

public class AVOutputFormat extends Struct {
    public AVOutputFormat(Runtime runtime) {
        super(runtime);
    }

    public Struct.String name = new AsciiStringRef();
    public Struct.String long_name = new AsciiStringRef();
    public Struct.String mime = new AsciiStringRef();
    public Struct.String extensions = new AsciiStringRef();

}