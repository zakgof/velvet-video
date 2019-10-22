package com.zakgof.velvetvideo;

public interface IVideoEncoderBuilder extends IEncoderBuilder<IVideoEncoderBuilder> {
	IVideoEncoderBuilder dimensions(int width, int height);
}