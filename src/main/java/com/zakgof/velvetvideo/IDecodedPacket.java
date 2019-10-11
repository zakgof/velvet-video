package com.zakgof.velvetvideo;

public interface IDecodedPacket {
	default IVideoFrame video() {
		return null;
	}
	default boolean isVideo() {
		return false;
	}
	// boolean isAudio();
	// TODO: enum ?
}