package com.zakgof.velvetvideo;

import javax.sound.sampled.AudioFormat;

/**
 * Provides audio stream properties.
 */
public interface IAudioStreamProperties {

	/** @return codec name */
	String codec();

	/** @return total stream duration, in nanoseconds */
	long nanoduration();

	/** @return total number of samples */
	long samples();

	/** @return stream audio format */
	AudioFormat format();
}
