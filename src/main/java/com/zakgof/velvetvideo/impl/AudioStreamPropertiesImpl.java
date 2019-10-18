package com.zakgof.velvetvideo.impl;

import javax.sound.sampled.AudioFormat;

import com.zakgof.velvetvideo.IAudioStreamProperties;

import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value
class AudioStreamPropertiesImpl implements IAudioStreamProperties {

	private final String codec;
	private final AudioFormat format;
	private final long nanoduration;
	private final long samples;

}
