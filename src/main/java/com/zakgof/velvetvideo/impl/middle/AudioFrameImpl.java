package com.zakgof.velvetvideo.impl.middle;

import com.zakgof.velvetvideo.IAudioFrame;
import com.zakgof.velvetvideo.IDecoderAudioStream;

import lombok.Value;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Value
class AudioFrameImpl implements IAudioFrame {

	private final byte[] samples;
	private final long nanostamp;
    private final long nanoduration;
    private final IDecoderAudioStream stream;

    @Override
	public String toString() {
    	return "Audio frame t=" + nanostamp + " stream:" + stream.name();
    }
}