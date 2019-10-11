package com.zakgof.velvetvideo.middle;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

import com.zakgof.velvetvideo.JNRHelper;
import com.zakgof.velvetvideo.VelvetVideoException;
import com.zakgof.velvetvideo.jnr.AVFrame;
import com.zakgof.velvetvideo.jnr.AVPixelFormat;
import com.zakgof.velvetvideo.jnr.LibAVUtil;
import com.zakgof.velvetvideo.jnr.LibSwScale;
import com.zakgof.velvetvideo.jnr.SwsContext;

public class FrameHolder implements AutoCloseable {

    public final AVFrame frame;
    private final AVFrame biframe;
    private final SwsContext scaleCtx;
    private final int width;
    private final int height;
	private static final LibSwScale libswscale = JNRHelper.load(LibSwScale.class, "swscale-5");
	private static final LibAVUtil libavutil = JNRHelper.load(LibAVUtil.class, "avutil-56");

    public FrameHolder(int width, int height, AVPixelFormat srcFormat, AVPixelFormat destFormat, boolean encode) {
        this.width = width;
        this.height = height;
        this.frame = alloc(width, height, encode ? destFormat : srcFormat);
        this.biframe = alloc(width, height, encode ? srcFormat : destFormat);
        scaleCtx = libswscale.sws_getContext(width, height, srcFormat, width, height, destFormat, 0, 0, 0, 0);
    }

    private AVFrame alloc(int width, int height, AVPixelFormat format) {
        AVFrame f = libavutil.av_frame_alloc();
        f.width.set(width);
        f.height.set(height);
        f.pix_fmt.set(format);
        libavutil.checkcode(libavutil.av_frame_get_buffer(f, 0));
        return f;
    }

    public AVFrame setPixels(BufferedImage image) {
        byte[] bytes = bytesOf(image);
        biframe.data[0].get().put(0, bytes, 0, bytes.length);
        libavutil.checkcode(libswscale.sws_scale(scaleCtx, JNRHelper.ptr(biframe.data[0]), JNRHelper.ptr(biframe.linesize[0]), 0, height,
                                       JNRHelper.ptr(frame.data[0]), JNRHelper.ptr(frame.linesize[0])));
        return frame;
    }

    public BufferedImage getPixels() {
    	libavutil.checkcode(libswscale.sws_scale(scaleCtx, JNRHelper.ptr(frame.data[0]), JNRHelper.ptr(frame.linesize[0]), 0, height,
                                       JNRHelper.ptr(biframe.data[0]), JNRHelper.ptr(biframe.linesize[0])));
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] bytes = bytesOf(bi);
        biframe.data[0].get().get(0,  bytes, 0, bytes.length);
        return bi;

    }

    private static byte[] bytesOf(BufferedImage image) {
        Raster raster = image.getRaster();
        DataBuffer buffer = raster.getDataBuffer();
        if (buffer instanceof DataBufferByte) {
            return ((DataBufferByte) buffer).getData();
        }
        throw new VelvetVideoException("Unsupported image data buffer type");
    }

    @Override
	public void close() {
       libavutil.av_frame_free(new AVFrame[] {frame});
       libavutil.av_frame_free(new AVFrame[] {biframe});
       libswscale.sws_freeContext(scaleCtx);
    }

}