package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import com.zakgof.velvetvideo.IVideoLib.IDecodedPacket;
import com.zakgof.velvetvideo.IVideoLib.IDemuxer;
import com.zakgof.velvetvideo.IVideoLib.IMuxer;

public class VelvetVideoTest {

	protected IVideoLib lib = new FFMpegVideoLib();
	protected static Path dir;

	@BeforeAll
	private static void setup() throws IOException {
		dir = Files.createTempDirectory("velvet-video-test-");
	}

	@AfterAll
	private static void cleanup() {
		dir.toFile().delete();
	}

	@AfterEach
	private void clean() {
		for (File file : dir.toFile().listFiles())
			file.delete();
	}

	protected static BufferedImage colorImage(int seed) {
		BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR);
		DataBufferByte dataBuffer = (DataBufferByte) image.getRaster().getDataBuffer();
		byte[] bytes = dataBuffer.getData();
		for (int x = 0; x < 640; x++) {
			for (int y = 0; y < 480; y++) {
				int offset = (x + y * 640) * 3;
				bytes[offset] = (byte) ((int) (127 + 127 * Math.sin(x * 0.12 / (seed + 1))) & 0xFF);
				bytes[offset + 1] = (byte) ((int) (127 + 127 * Math.sin(y * 0.081 / (seed + 1))) & 0xFF);
				bytes[offset + 2] = (byte) ((int) (127 + 127 * Math.sin((x + y) * 0.01 / (seed + 1))) & 0xFF);
			}
		}
		return image;
	}

	protected static BufferedImage bwImage(int seed) {
		BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR);
		DataBufferByte dataBuffer = (DataBufferByte) image.getRaster().getDataBuffer();
		byte[] bytes = dataBuffer.getData();
		for (int x = 0; x < 640; x++) {
			for (int y = 0; y < 480; y++) {
				int offset = (x + y * 640) * 3;
				bytes[offset] = bytes[offset + 1] = bytes[offset
						+ 2] = (byte) ((int) (127 + 127 * Math.sin((x - y) * 0.64 / (seed + 1))) & 0xFF);
			}
		}
		return image;
	}

	protected double diff(BufferedImage im1, BufferedImage im2) {
		byte[] bytes1 = ((DataBufferByte) im1.getRaster().getDataBuffer()).getData();
		byte[] bytes2 = ((DataBufferByte) im2.getRaster().getDataBuffer()).getData();
		double diff = 0;
		for (int s = 0; s < bytes1.length; s++) {
			double delta = bytes1[s] - bytes2[s];
			diff += Math.sqrt(delta * delta);
		}
		return diff / bytes1.length;
	}

	protected void assertEqual(BufferedImage im1, BufferedImage im2) {
		double diff = diff(im1, im2);
		Assertions.assertEquals(0, diff, 1.0);
	}

	protected BufferedImage[] createSingleStreamVideo(String codec, String format, File file, int frames) {
		BufferedImage[] orig = new BufferedImage[frames];
		try (IMuxer muxer = lib.muxer(format)
				.video("dflt", lib.encoder(codec)
					.bitrate(3000000)
					.dimensions(640, 480)
					.framerate(30)
					.enableExperimental())
				.build(file)) {
			for (int i = 0; i < orig.length; i++) {
				BufferedImage image = colorImage(i);
				muxer.video("dflt").encode(image, i);
				orig[i] = image;
			}
		}
		return orig;
	}

	protected BufferedImage[][] createTwoStreamVideo(File file, int frames) {
		System.err.println(file);
		BufferedImage[][] origs = { new BufferedImage[frames], new BufferedImage[frames] };
		try (IMuxer muxer = lib.muxer("mp4")
				.video("color", lib.encoder("mpeg4")
			        .dimensions(640, 480)
				    .framerate(1))
				.video("bw", lib.encoder("mpeg4")
				     .dimensions(640, 480)
				     .framerate(1))
				.build(file)) {

			for (int i = 0; i < frames; i++) {
				BufferedImage color = colorImage(i);
				BufferedImage bw = bwImage(i);
				muxer.video("color").encode(color, i);
				muxer.video("bw").encode(bw, i);
				origs[0][i] = color;
				origs[1][i] = bw;
			}
		}
		return origs;
	}

	protected List<BufferedImage> loadFrames(File file, int frames) {
		List<BufferedImage> restored = new ArrayList<>(frames);
		try (IDemuxer demuxer = lib.demuxer(file)) {
			for (IDecodedPacket packet : demuxer) {
				restored.add(packet.video().image());
			}
		}
		Assertions.assertEquals(frames, restored.size());
		return restored;
	}
}
