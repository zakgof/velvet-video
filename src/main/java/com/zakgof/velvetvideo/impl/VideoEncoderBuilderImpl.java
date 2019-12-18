package com.zakgof.velvetvideo.impl;

import com.zakgof.velvetvideo.IVideoDecoderStream;
import com.zakgof.velvetvideo.IVideoEncoderBuilder;

class VideoEncoderBuilderImpl extends AbstractEncoderBuilderImpl<IVideoEncoderBuilder> implements IVideoEncoderBuilder {

	Integer width;
	Integer height;

	public VideoEncoderBuilderImpl(String codec) {
		super(codec);
	}

	VideoEncoderBuilderImpl(IVideoDecoderStream decoder) {
		super(decoder);
	}

	@Override
	public IVideoEncoderBuilder dimensions(int width, int height) {
		this.width = width;
		this.height = height;
		return this;
	}

}