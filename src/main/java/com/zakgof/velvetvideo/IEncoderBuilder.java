package com.zakgof.velvetvideo;

/**
 * Builder for encoding media streams.
 *
 * @param <I> more specific builder interface, {@link IVideoEncoderBuilder} or {@link IAudioEncoderBuilder}
 */
public interface IEncoderBuilder<I extends IEncoderBuilder<?>> {

	/**
	 * Sets codec's frame rate (time base) as a rational number.
	 * @param num frame duration numerator
	 * @param den frame duration denominator
	 * @return this builder
	 */
	I framerate(int num, int den);

	/**
	 * Sets codec's frame rate (time base). Equivalent to <code>framerate(1, framerate)</code>
	 * @param framerate frame rate in frames per seconds.
	 * @return this builder
	 */
	I framerate(int framerate);

	/**
	 * Sets a codec parameter. Refer to ffmpeg documentation for the specific codec for details.
	 * @param key parameter name
	 * @param value parameter value
	 * @return this builder
	 */
	I param(String key, String value);

	/**
	 * Sets a metadata value.
	 * @param key metadata key name
	 * @param value metadata value
	 * @return this builder
	 */
	I metadata(String key, String value);

	/**
	 * Enables codec's experimental features. Refer to ffmpeg's codec specific documentation for details.
	 * @return this builder.
	 */
	I enableExperimental();

	/**
	 * Sets a pre-encoding filter.
	 * @param filter filters string as specified by ffmpeg documentation for filters.
	 * @return this builder.
	 */
	I filter(String filter);

	/**
	 * Sets bitrate for CBR encoding. The effect is codec-specific.
	 * @param bitrate bitrate in KBytes/s or kbits/s (see the specific codec documentation for details).
	 * @return this builder
	 */
	I bitrate(int bitrate);
}
