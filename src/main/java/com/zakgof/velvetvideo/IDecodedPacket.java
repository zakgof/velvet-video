package com.zakgof.velvetvideo;

public interface IDecodedPacket {
	default IVideoFrame video() {
		return null;
	}

	default boolean isVideo() {
		return false;
	}

	default IAudioFrame audio() {
		return null;
	}

	default boolean isAudio() {
		return false;
	}
	// TODO: enum ?
}