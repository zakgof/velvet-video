package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class AVInputFormat extends Struct {
    public AVInputFormat(Runtime runtime) {
        super(runtime);
    }

    public Struct.String name = new AsciiStringRef();
    public Struct.String long_name = new AsciiStringRef();
}