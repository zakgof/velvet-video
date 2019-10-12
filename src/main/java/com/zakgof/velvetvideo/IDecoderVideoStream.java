package com.zakgof.velvetvideo;

import java.util.Map;

public interface IDecoderVideoStream {
	String name();

	IVideoFrame nextFrame();

	Map<String, String> metadata();

	IVideoStreamProperties properties();

	IDecoderVideoStream seek(long frameNumber);

	IDecoderVideoStream seekNano(long ns);

	byte[] nextRawPacket();

	int index();

	// TODO: mutator does not look good here
	void setFilter(String filterString);
}