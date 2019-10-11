package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;

public interface IFrame {
    IDecoderVideoStream stream();
    BufferedImage image();
    long nanostamp();
	long nanoduration();
}