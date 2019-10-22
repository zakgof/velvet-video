package com.zakgof.velvetvideo.impl.middle;

import com.zakgof.velvetvideo.IDecodedPacket;
import com.zakgof.velvetvideo.IVideoFrame;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@RequiredArgsConstructor
class DecodedVideoPacket implements IDecodedPacket {
	private final IVideoFrame frame;

	@Override
	public IVideoFrame video() {
		return frame;
	}

	@Override
	public boolean isVideo() {
		return true;
	}
}