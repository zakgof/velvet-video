package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;

/**
 * Muxer's video stream.
 */
public interface IVideoEncoderStream {

	/**
	 * Add video frame with default duration for encoding. Frame duration will be 1/codecframerate. Equivalent to <code>encode(image, 1)</code>
	 * @param image video frame image
	 */
	void encode(BufferedImage image);

	/**
	 * Add video frame with the specified duration for encoding. Frame duration will be duration/codecframerate.
	 * @param image video frame image
	 * @param duration frame duration, in codec time base units.
	 */
	void encode(BufferedImage image, int duration);
}