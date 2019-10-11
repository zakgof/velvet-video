package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;

public interface IVideoFrame {
    IDecoderVideoStream stream();
    BufferedImage image();
    long nanostamp();
	long nanoduration();
}