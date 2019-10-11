package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class SwsContext extends Struct {
    public SwsContext(Runtime runtime) {
        super(runtime);
    }
}