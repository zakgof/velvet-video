package com.zakgof.velvetvideo;

/**
 * Builder for video stream {@link IVideoEncoderStream}
 */
public interface IVideoEncoderBuilder extends IEncoderBuilder<IVideoEncoderBuilder> {

	/**
	 * Sets video frame dimensions.
	 * @param width width, in pixels
	 * @param height height, in pixels
	 * @return this builder
	 */
	IVideoEncoderBuilder dimensions(int width, int height);
}