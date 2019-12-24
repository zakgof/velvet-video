package com.zakgof.velvetvideo;

public interface IAudioEncoderStream {

	void encode(byte[] samples);

	void encode(byte[] samples, int offset);

	int frameBytes();

}