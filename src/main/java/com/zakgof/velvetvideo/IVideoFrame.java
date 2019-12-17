package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;

public interface IVideoFrame extends IDecodedPacket<IDecoderVideoStream> {

	BufferedImage image();

	@Override
	default MediaType type() {
		return MediaType.Video;
	}

	@Override
	default IVideoFrame asVideo() {
		return this;
	}
}