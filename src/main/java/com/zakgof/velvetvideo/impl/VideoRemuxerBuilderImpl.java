package com.zakgof.velvetvideo.impl;

import com.zakgof.velvetvideo.IDecoderVideoStream;
import com.zakgof.velvetvideo.IVideoRemuxerBuilder;

public class VideoRemuxerBuilderImpl implements IVideoRemuxerBuilder {

	Integer timebaseNum;
	Integer timebaseDen;
	final IDecoderVideoStream decoder;

	public VideoRemuxerBuilderImpl(IDecoderVideoStream decoder) {
		this.decoder = decoder;
	}

	@Override
	public VideoRemuxerBuilderImpl framerate(int framerate) {
		this.timebaseNum = 1;
		this.timebaseDen = framerate;
		return this;
	}

	@Override
	public VideoRemuxerBuilderImpl framerate(int num, int den) {
		this.timebaseNum = num;
		this.timebaseDen = den;
		return this;
	}

}
