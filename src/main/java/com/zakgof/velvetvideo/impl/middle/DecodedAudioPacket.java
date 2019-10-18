package com.zakgof.velvetvideo.impl.middle;

import com.zakgof.velvetvideo.IAudioFrame;
import com.zakgof.velvetvideo.IDecodedPacket;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@RequiredArgsConstructor
class DecodedAudioPacket implements IDecodedPacket {
	private final IAudioFrame frame;

	@Override
	public IAudioFrame audio() {
		return frame;
	}

	@Override
	public boolean isAudio() {
		return true;
	}
}