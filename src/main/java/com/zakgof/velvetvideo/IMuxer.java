package com.zakgof.velvetvideo;

public interface IMuxer extends AutoCloseable {

	@Override
	void close();

	IEncoderVideoStream video(int index);
}