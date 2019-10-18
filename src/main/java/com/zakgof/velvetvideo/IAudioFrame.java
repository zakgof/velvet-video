package com.zakgof.velvetvideo;

public interface IAudioFrame {
	byte[] samples();

	long nanostamp();

	long nanoduration();

	IDecoderAudioStream stream();
}