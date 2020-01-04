package com.zakgof.velvetvideo;

/**
 * Properties of a video stream.
 */
public interface IVideoStreamProperties {

	/**
	 * @return codec name
	 */
	String codec();

	/**
	 * @return framerate, frames per second
	 */
	// TODO return some nicer rational
	double framerate();

	/**
	 * @return total duration of the stream, in nanoseconds
	 */
	long nanoduration();

	/**
	 * @return total number of video frames
	 */
	long frames();

	/**
	 * @return video width, in pixels
	 */
	int width();

	/**
	 * @return video height, in pixels
	 */
	int height();
}