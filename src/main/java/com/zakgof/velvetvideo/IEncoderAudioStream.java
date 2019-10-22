package com.zakgof.velvetvideo;

public interface IEncoderAudioStream {

	void encode(byte[] samples);

	void encode(byte[] samples, int offset);

	int frameBytes();

}