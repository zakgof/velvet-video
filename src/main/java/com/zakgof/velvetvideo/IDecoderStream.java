package com.zakgof.velvetvideo;

import java.util.Map;

public interface IDecoderStream<I extends IDecoderStream<?, ?, ?>, F, P> {
	String name();

	F nextFrame();

	Map<String, String> metadata();

	P properties();

	I seek(long frameNumber);

	I seekNano(long ns);

	byte[] nextRawPacket();

	int index();

	// TODO: mutator does not look good here
	void setFilter(String filterString);
}
