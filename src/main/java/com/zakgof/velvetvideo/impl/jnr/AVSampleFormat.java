package com.zakgof.velvetvideo.impl.jnr;

import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

import com.zakgof.velvetvideo.VelvetVideoException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum AVSampleFormat {

	AV_SAMPLE_FMT_U8(Encoding.PCM_UNSIGNED, 8, false),
	AV_SAMPLE_FMT_S16(Encoding.PCM_SIGNED, 16, false),
	AV_SAMPLE_FMT_S32(Encoding.PCM_SIGNED, 32, false),
	AV_SAMPLE_FMT_FLT(Encoding.PCM_FLOAT, 32, false),
	AV_SAMPLE_FMT_DBL(Encoding.PCM_FLOAT, 64, false),
	AV_SAMPLE_FMT_U8P(Encoding.PCM_UNSIGNED, 8, true),
	AV_SAMPLE_FMT_S16P(Encoding.PCM_SIGNED, 16, true),
	AV_SAMPLE_FMT_S32P(Encoding.PCM_SIGNED, 32, true),
	AV_SAMPLE_FMT_FLTP(Encoding.PCM_FLOAT, 32, true),
	AV_SAMPLE_FMT_DBLP(Encoding.PCM_FLOAT, 64, true),
	AV_SAMPLE_FMT_S64(Encoding.PCM_SIGNED, 64, false),
	AV_SAMPLE_FMT_S64P(Encoding.PCM_SIGNED, 64, true),
	;

	private final Encoding encoding;
	private final int sampleSizeInBits;
	private final boolean planar;

	public AudioFormat toAudioFormat(int frameRate, int channels) {
		int frameSize = (sampleSizeInBits >> 3) * (planar ? 1 : channels);
		return new AudioFormat(encoding, frameRate, sampleSizeInBits, channels, frameSize, frameRate, false);
	}

	public AVSampleFormat destFormat() {
		switch(this) {
			case AV_SAMPLE_FMT_U8:
			case AV_SAMPLE_FMT_S16:
			case AV_SAMPLE_FMT_S32:
			case AV_SAMPLE_FMT_FLT:
			case AV_SAMPLE_FMT_DBL:
			case AV_SAMPLE_FMT_S64:
				return this;

			case AV_SAMPLE_FMT_U8P: return AV_SAMPLE_FMT_U8;
			case AV_SAMPLE_FMT_S16P: return AV_SAMPLE_FMT_S16;
			case AV_SAMPLE_FMT_S32P: return AV_SAMPLE_FMT_S32;
			case AV_SAMPLE_FMT_FLTP: return AV_SAMPLE_FMT_FLT;
			case AV_SAMPLE_FMT_DBLP: return AV_SAMPLE_FMT_DBL;
			case AV_SAMPLE_FMT_S64P: return AV_SAMPLE_FMT_S64;
	 	}
		throw new VelvetVideoException("Unsupported sample format " + this);
	}

	public int bytesPerSample() {
		return sampleSizeInBits >> 3;
	}

	public static AVSampleFormat from(AudioFormat targetFormat) {
		return Arrays.stream(values()).filter(sf -> sf.encoding == targetFormat.getEncoding() && sf.sampleSizeInBits == targetFormat.getSampleSizeInBits() && !sf.planar).findFirst().get();
	}
}
