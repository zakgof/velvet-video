package com.zakgof.velvetvideo;

public interface IAudioFrame extends IDecodedPacket<IAudioDecoderStream> {
	byte[] samples();

	@Override
	default MediaType type() {
		return MediaType.Audio;
	}

	@Override
	default IAudioFrame asAudio() {
		return this;
	}
}