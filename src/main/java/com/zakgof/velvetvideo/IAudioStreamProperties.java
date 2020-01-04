package com.zakgof.velvetvideo;

import javax.sound.sampled.AudioFormat;

/**
 * Provides audio stream properties.
 */
public interface IAudioStreamProperties {

	/** Codec name */
	String codec();

	/** Total stream duration, in nanoseconds */
	long nanoduration();

	/** Total number of samples */
	long samples();

	/** Stream audio format */
	AudioFormat format();
}
