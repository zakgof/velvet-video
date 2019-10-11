package com.zakgof.velvetvideo;

public interface IMuxer extends AutoCloseable {

	@Override
	void close();

	IEncoderVideoStream videoEncoder(int index);

	IRemuxerVideoStream videoRemuxer(int index);
}