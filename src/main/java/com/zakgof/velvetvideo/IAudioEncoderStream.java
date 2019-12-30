package com.zakgof.velvetvideo;

/**
 * Muxer audio stream.
 */
public interface IAudioEncoderStream {

	/**
	 * Send audio samples for encoding. The length should not exceed frame size returned by {@link #frameBytes()}.
	 * @param samples byte arrays with samples to send
	 */
	// TODO
	void encode(byte[] samples);

	/**
	 * Send audio samples for encoding. The length should not exceed frame size returned by {@link #frameBytes()}.
	 * @param samples byte arrays with samples to send
	 * @param offset number of bytes in the beginning of array to skip
	 */
	// TODO
	void encode(byte[] samples, int offset);

	/**
	 * @return audio frame size, in bytes. Audio frame samples sent in {@link #encode(byte[])} should have this length (except the last frame that may be shorter).
	 */
	int frameBytes();

}