package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.util.List;

public interface IVideoLib {

	List<String> codecs();
	IEncoder.IBuilder encoderBuilder(int width, int height, IEncoderStream output);

	interface IEncoder extends AutoCloseable {
		
		void encode(BufferedImage bi, long pts);
		void close();

		interface IBuilder {
			IBuilder framerate(int framerate);
			IBuilder bitrate(int bitrate);
			IBuilder codec(String codec);
			IBuilder param(String key, String value);
			IEncoder build();
		}

	}
	
	interface IEncoderStream extends AutoCloseable {
		void send(byte[] bytes);
		void close();
	}
}
