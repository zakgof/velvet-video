package com.zakgof.velvetvideo.jnr;

import jnr.ffi.Pointer;

public interface LibSwScale {
    SwsContext sws_getContext(int width, int height, AVPixelFormat avPixFmtRgb24, int width2, int height2, AVPixelFormat avPixFmtYuv420p, int i, int j, int k, int l);
    int sws_scale(SwsContext ctx, Pointer inData, Pointer inStride, int srcSliceY, int height, Pointer outData, Pointer outStride);
    void sws_freeContext(SwsContext swsContext);
}