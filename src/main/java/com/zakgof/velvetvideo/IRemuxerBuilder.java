package com.zakgof.velvetvideo;

/**
 * Builder for a media stream that writes raw (encoded) packets without
 * transcoding.
 */
public interface IRemuxerBuilder {

	/**
	 * Sets codec's frame rate (time base) as a rational number.
	 *
	 * @param num frame duration numerator
	 * @param den frame duration denominator
	 * @return this builder
	 */
	IRemuxerBuilder framerate(int num, int den);

	/**
	 * Sets codec's frame rate (time base). Equivalent to
	 * <code>framerate(1, framerate)</code>
	 *
	 * @param framerate frame rate in frames per seconds.
	 * @return this builder
	 */
	IRemuxerBuilder framerate(int framerate);

}
