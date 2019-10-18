package com.zakgof.velvetvideo;

import java.util.Map;

public interface IDecoderAudioStream {
	String name();

	IAudioFrame nextFrame();

	Map<String, String> metadata();

	IAudioStreamProperties properties();

	IDecoderAudioStream seek(long frameNumber);

	IDecoderAudioStream seekNano(long ns);

	byte[] nextRawPacket();

	int index();

	// TODO: mutator does not look good here
	void setFilter(String filterString);
}
