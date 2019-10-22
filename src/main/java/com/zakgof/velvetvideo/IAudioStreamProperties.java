package com.zakgof.velvetvideo;

import javax.sound.sampled.AudioFormat;

public interface IAudioStreamProperties {
	String codec();

	long nanoduration();

	long samples();

	AudioFormat format();
}
