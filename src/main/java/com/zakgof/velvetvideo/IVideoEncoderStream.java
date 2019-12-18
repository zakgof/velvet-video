package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;

public interface IVideoEncoderStream {

	void encode(BufferedImage image);

	void encode(BufferedImage image, int duration);

}