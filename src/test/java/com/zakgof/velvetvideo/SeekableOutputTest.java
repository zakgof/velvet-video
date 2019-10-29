package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SeekableOutputTest extends VelvetVideoTest {

	private static final int FRAMES = 30;

	@Test
	public void testMemOutput() throws IOException {
    	File file = dir.resolve("file.mp4").toFile();

    	IMuxerBuilder builder = lib.muxer("mp4")
		.videoEncoder(lib.videoEncoder("libx264")
			.bitrate(3000000)
			.dimensions(640, 480)
			.framerate(25)
			.enableExperimental());

    	try (IMuxer muxer = builder.build(file)) {
    		for (int i = 0; i < FRAMES; i++) {
				BufferedImage image = colorImage(i);
				muxer.videoEncoder(0).encode(image);
			}
    	}
    	MemSeekableFile msf = new MemSeekableFile();
    	try (IMuxer muxer = builder.build(msf)) {
    		for (int i = 0; i < FRAMES; i++) {
				BufferedImage image = colorImage(i);
				muxer.videoEncoder(0).encode(image);
			}
    	}
    	byte[] fileloaded = Files.readAllBytes(file.toPath());
    	byte[] msfbytes = msf.toBytes();
    	Assertions.assertArrayEquals(fileloaded, msfbytes);
	}
}