package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;

/**
 * Decoded video frame.
 */
public interface IVideoFrame extends IDecodedPacket<IVideoDecoderStream> {

	/**
	 * @return video frame image
	 */
	BufferedImage image();

	@Override
	default MediaType type() {
		return MediaType.Video;
	}

	@Override
	default IVideoFrame asVideo() {
		return this;
	}
}