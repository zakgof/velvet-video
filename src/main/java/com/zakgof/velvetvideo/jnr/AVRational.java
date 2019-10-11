package com.zakgof.velvetvideo.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.Struct.Signed32;

public class AVRational extends Struct {

    public AVRational(Runtime runtime) {
        super(runtime);
    }

    public Signed32 num = new Signed32();
    public Signed32 den = new Signed32();
}