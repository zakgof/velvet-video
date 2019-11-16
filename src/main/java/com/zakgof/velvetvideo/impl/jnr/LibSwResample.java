package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Pointer;

public interface LibSwResample {

	Pointer swr_alloc();

	int swr_init(Pointer swr);

	int swr_convert(Pointer swrContext, Pointer[] out, int out_count, Pointer in, int in_count);
	int swr_convert(Pointer swrContext, Pointer out, int out_count, Pointer[] in, int in_count);

	void swr_free(Pointer[] pointers);

}
