package com.zakgof.velvetvideo;

public interface IMuxer extends AutoCloseable {

	@Override
	void close();

	IEncoderVideoStream videoEncoder(int index);

	IEncoderAudioStream audioEncoder(int index);

	IRemuxerStream remuxer(int index);
}