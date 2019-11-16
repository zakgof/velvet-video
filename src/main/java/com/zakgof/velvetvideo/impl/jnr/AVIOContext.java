package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class AVIOContext extends Struct {
	public AVIOContext(Runtime runtime) {
		super(runtime);
	}

	Pointer av_class = new Pointer();
	public Pointer buffer = new Pointer();
}