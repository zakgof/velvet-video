package com.zakgof.velvetvideo;

/**
 * Decoded media data frame.
 *
 * @param <S> media decoder stream type, {@link IAudioDecoderStream} or {@link IVideoDecoderStream}
 */
public interface IDecodedPacket<S extends IDecoderStream<?, ?, ?>> {

	/**
	 * @return media type, video or audio
	 */
	MediaType type();

	/**
	 * Checks if media is of a specific type.
	 * @param mediaType suggested video type
	 * @return true is type matches, false if not
	 */
	default boolean is(MediaType mediaType) {
		return type() == mediaType;
	}

	/**
	 * @return {@link IVideoFrame} for this packet, or null if the packet is not video
	 */
	default IVideoFrame asVideo() {
		return null;
	}

	/**
	 * @return {@link IAudioFrame} for this packet, or null if the packet is not audio
	 */
	default IAudioFrame asAudio() {
		return null;
	}

	/**
	 * @return media stream
	 */
	S stream();

	/**
	 * @return time offset of this frame from the stream start, in nanoseconds
	 */
	long nanostamp();

	/**
	 * @return duration of this frame, in nanoseconds
	 */
	long nanoduration();

}