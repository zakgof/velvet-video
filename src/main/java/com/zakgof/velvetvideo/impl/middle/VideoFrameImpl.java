package com.zakgof.velvetvideo.impl.middle;

import java.awt.image.BufferedImage;

import com.zakgof.velvetvideo.IVideoDecoderStream;
import com.zakgof.velvetvideo.IVideoFrame;

import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value
class VideoFrameImpl implements IVideoFrame {
    private final BufferedImage image;
    private final long nanostamp;
    private final long nanoduration;
    private final IVideoDecoderStream stream;

    @Override
	public String toString() {
    	return "Video frame t=" + nanostamp + " stream:" + stream.name();
    }
}