package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class AVOption extends Struct {
    public AVOption(Runtime runtime) {
        super(runtime);
    }
    public String name = new AsciiStringRef();
}
