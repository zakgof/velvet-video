package com.zakgof.velvetvideo;

public interface IDecodedPacket<S extends IDecoderStream<?, ?, ?>> {

	MediaType type();

	default boolean is(MediaType mediaType) {
		return type() == mediaType;
	}

	default IVideoFrame asVideo() {
		return null;
	}

	default IAudioFrame asAudio() {
		return null;
	}

	S stream();

	long nanostamp();

	long nanoduration();

}