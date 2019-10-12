package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import com.zakgof.velvetvideo.impl.VelvetVideoLib;

public class VelvetVideoTest {

	protected IVelvetVideoLib lib = new VelvetVideoLib();
	protected static Path dir;

	@BeforeAll
	private static void setup() throws IOException {
		dir = Files.createTempDirectory("velvet-video-test-");
	}

//	@AfterAll
	private static void cleanup() {
		dir.toFile().delete();
	}

//	@AfterEach
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

	protected static BufferedImage colorImageNoisy(int seed) {
		BufferedImage image = colorImage(seed);
		DataBufferByte dataBuffer = (DataBufferByte) image.getRaster().getDataBuffer();
		byte[] bytes = dataBuffer.getData();
		Random r = new Random(seed * 50);
		for (int i=0; i<bytes.length; i++) {
			if (r.nextInt(20) == 0 )
				bytes[i] += r.nextInt(50);
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
		assertEqual(im1, im2, 1.0);
	}

	protected void assertEqual(BufferedImage im1, BufferedImage im2, double tolerance) {
		Assertions.assertEquals(im1.getWidth(), im2.getWidth());
		Assertions.assertEquals(im1.getHeight(), im2.getHeight());
		double diff = diff(im1, im2);
		Assertions.assertEquals(0, diff, tolerance);
	}

	protected BufferedImage[] createSingleStreamVideo(String codec, String format, File file, int frames) {
		BufferedImage[] orig = new BufferedImage[frames];
		try (IMuxer muxer = lib.muxer(format)
				.videoEncoder(lib.videoEncoder(codec)
					.bitrate(3000000)
					.dimensions(640, 480)
					.framerate(25)
					.enableExperimental())
				.build(file)) {
			for (int i = 0; i < orig.length; i++) {
				BufferedImage image = colorImage(i);
				muxer.videoEncoder(0).encode(image);
				orig[i] = image;
			}
		}
		return orig;
	}

	protected BufferedImage[] createVariableFrameDurationVideo(String codec, String format, File file, int frames) {
		BufferedImage[] orig = new BufferedImage[frames];
		try (IMuxer muxer = lib.muxer(format)
				.videoEncoder(lib.videoEncoder(codec)
					.bitrate(4000000)
					.dimensions(640, 480)
					.framerate(50)
					.enableExperimental())
				.build(file)) {
			for (int i = 0; i < orig.length; i++) {
				BufferedImage image = colorImage(i);
				muxer.videoEncoder(0).encode(image, i);
				orig[i] = image;
			}
		}
		return orig;
	}

	protected BufferedImage[][] createTwoStreamVideo(File file, int frames, String codec, String format) {
		System.err.println(file);
		BufferedImage[][] origs = { new BufferedImage[frames], new BufferedImage[frames] };
		try (IMuxer muxer = lib.muxer(format)
				.videoEncoder(lib.videoEncoder(codec)
			        .dimensions(640, 480)
				    .framerate(30))
				.videoEncoder(lib.videoEncoder(codec)
				     .dimensions(640, 480)
				     .framerate(30))
				.build(file)) {

			for (int i = 0; i < frames; i++) {
				BufferedImage color = colorImage(i);
				BufferedImage bw = bwImage(i);
				muxer.videoEncoder(0).encode(color);
				muxer.videoEncoder(1).encode(bw);
				origs[0][i] = color;
				origs[1][i] = bw;
			}
		}
		return origs;
	}

	protected List<BufferedImage> loadFrames(File file, int frames) {
		return loadFrames(file, frames, null);
	}

	protected List<BufferedImage> loadFrames(File file, int frames, String filter) {
		List<BufferedImage> restored = new ArrayList<>(frames);
		try (IDemuxer demuxer = lib.demuxer(file)) {
			demuxer.videoStream(0).setFilter(filter);
			for (IDecodedPacket packet : demuxer) {
				restored.add(packet.video().image());
			}
		}
		Assertions.assertEquals(frames, restored.size());
		return restored;
	}
}
