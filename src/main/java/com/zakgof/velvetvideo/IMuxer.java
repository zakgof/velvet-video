package com.zakgof.velvetvideo;

public interface IMuxer extends AutoCloseable {

	@Override
	void close();

	IVideoEncoderStream videoEncoder(int index);

	IAudioEncoderStream audioEncoder(int index);

	IRemuxerStream remuxer(int index);
}