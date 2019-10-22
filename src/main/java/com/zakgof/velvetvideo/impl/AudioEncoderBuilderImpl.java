package com.zakgof.velvetvideo.impl;

import javax.sound.sampled.AudioFormat;

import com.zakgof.velvetvideo.IAudioEncoderBuilder;

class AudioEncoderBuilderImpl extends AbstractEncoderBuilderImpl<IAudioEncoderBuilder> implements IAudioEncoderBuilder {

	AudioFormat inputFormat;

	public AudioEncoderBuilderImpl(String codec, AudioFormat inputFormat) {
		super(codec);
		this.inputFormat = inputFormat;
		framerate((int)inputFormat.getSampleRate());
	}

}