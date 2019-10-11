package com.zakgof.velvetvideo.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class SwsContext extends Struct {
    public SwsContext(Runtime runtime) {
        super(runtime);
    }
}