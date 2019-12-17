package com.zakgof.velvetvideo.impl.middle;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;

import com.zakgof.tools.generic.MinFinder;
import com.zakgof.velvetvideo.impl.jnr.AVSampleFormat;

public class BestMatchingAudioFormatConvertor implements Function<AudioFormat, AudioFormat> {

	private final Collection<AudioFormat> supportedFormats;

	public BestMatchingAudioFormatConvertor() {
		this(getSupportedFormats());
	}

	public BestMatchingAudioFormatConvertor(Collection<AudioFormat> supportedFormats) {
		this.supportedFormats = supportedFormats;
	}

	@Override
	public AudioFormat apply(AudioFormat suggested) {
		AudioFormat bestFormat = MinFinder.find(supportedFormats, format -> compare(suggested, format)).get();
		if (bestFormat != null) {
			return new AudioFormat(bestFormat.getEncoding(), suggested.getSampleRate(), bestFormat.getSampleSizeInBits(), bestFormat.getChannels(), bestFormat.getFrameSize(), suggested.getSampleRate(), bestFormat.isBigEndian());
		}
		return suggested;
	}

	private static float compare(AudioFormat suggested, AudioFormat format) {
		float metric = 0;

		int bitnessDiff = format.getSampleSizeInBits() - suggested.getSampleSizeInBits();
		if (bitnessDiff > 0) {
			metric += bitnessDiff * 100;
		} else {
			metric -= bitnessDiff * 1000;
		}

		int channelsDiff = format.getChannels() - suggested.getChannels();
		if (channelsDiff > 0) {
			metric += channelsDiff * 10000;
		} else {
			metric -= channelsDiff * 100000;
		}

		if (format.getEncoding() != suggested.getEncoding()) {
			metric += 10;
		}

		if (format.isBigEndian())
			metric += 1e6;

		// System.err.println("Metric " + metric + " for format " + format);

		return metric;
	}

	private static Collection<AudioFormat> getSupportedFormats() {
		return Arrays.stream(AudioSystem.getMixerInfo())
		    .map(AudioSystem::getMixer)
		    .map(Mixer::getTargetLineInfo)
		    .flatMap(Arrays::stream)
		    .filter(DataLine.Info.class::isInstance)
		    .map(DataLine.Info.class::cast)
		    .map(DataLine.Info::getFormats)
		    .flatMap(Arrays::stream)
		    .collect(Collectors.toSet());
	}

	public static AVSampleFormat findBest(Set<AVSampleFormat> supportedFormats, AudioFormat suggested) {
		AVSampleFormat bestFormat = MinFinder.find(supportedFormats, format -> compare(suggested, format.toAudioFormat(0, -1))).get();
		if (bestFormat != null) {
			return bestFormat;
		}
		return AVSampleFormat.from(suggested);
	}

}
