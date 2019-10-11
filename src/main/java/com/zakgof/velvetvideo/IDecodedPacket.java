package com.zakgof.velvetvideo;

public interface IDecodedPacket {
	default IFrame video() {
		return null;
	}
	default boolean isVideo() {
		return false;
	}
	// boolean isAudio();
	// TODO: enum ?
}