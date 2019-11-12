package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Pointer;

public interface LibSwScale {
    SwsContext sws_getContext(int srcW, int srcH, AVPixelFormat srcFormat, int dstW, int dstH, AVPixelFormat dstFormat, int flags, Pointer srcFilter, Pointer destFilter, Pointer param);
    int sws_scale(SwsContext ctx, Pointer inData, Pointer inStride, int srcSliceY, int height, Pointer outData, Pointer outStride);
    void sws_freeContext(SwsContext swsContext);
}