package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.opentest4j.AssertionFailedError;
import org.quifft.QuiFFT;
import org.quifft.output.FFTResult;
import org.quifft.params.WindowFunction;

import com.zakgof.velvetvideo.impl.VelvetVideoLib;

public class VelvetVideoTest {

	protected IVelvetVideoLib lib = new VelvetVideoLib();
	protected static Path dir;

	@BeforeAll
	private static void setup() throws IOException {
		String home = System.getProperty("user.home");
		dir = Paths.get(home, ".velvet-video", "test");
		dir.toFile().mkdirs();
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

	protected File file(String name) {
		return new File(dir.toFile(), name);
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
		double abs = Math.abs(diff);
		if (abs > tolerance) {
			saveImage(im1, file("im1.png"));
			saveImage(im2, file("im2.png"));
			Assertions.fail("images differ by " + abs);
		}
	}

	private void saveImage(BufferedImage im1, File file) {
		try {
			ImageIO.write(im1, "png", file);
		} catch (IOException e) {
			throw new AssertionFailedError("", e);
		}
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

	protected void assertAudioEqual(AudioFormat format, byte[] audio1, byte[] audio2) throws Exception {
		FFTResult fft1 = fft(format, audio1);
		FFTResult fft2 = fft(format, audio2);
		assertFftsEquals(fft1, fft2, 0.07);
	}

	private void assertFftsEquals(FFTResult fft1, FFTResult fft2, double tolerance) {
		double avgdiff = 0;
		Assertions.assertEquals(fft1.fftFrames.length, fft2.fftFrames.length);
		for (int i=0; i<fft1.fftFrames.length; i++) {
			Assertions.assertEquals(fft1.fftFrames[i].bins.length, fft2.fftFrames[i].bins.length);
			for (int b=0; b<fft1.fftFrames[i].bins.length; b++) {
				double diff = Math.abs((fft1.fftFrames[i].bins[b].amplitude - fft2.fftFrames[i].bins[b].amplitude)/fft2.fftFrames[i].bins[b].amplitude);
				avgdiff += diff / (fft1.fftFrames.length * fft1.fftFrames[i].bins.length);
			}
		}
		System.err.println("Audio file difference: " + avgdiff);
		Assertions.assertTrue(avgdiff < tolerance, "Audio waveforms differ too much: " + avgdiff);
	}

	private FFTResult fft(AudioFormat format, byte[] buf) throws Exception {
		File f = AudioUtil.saveWav(format, buf, file("tmp.wav"));
		QuiFFT quiFFT = new QuiFFT(f).windowFunction(WindowFunction.BLACKMAN).windowSize(4096).windowOverlap(0).normalized(true);
		return quiFFT.fullFFT();
	}

	protected byte[] readAudio(File file, int ms) {
		IVelvetVideoLib lib = new VelvetVideoLib();
		IDecoderAudioStream audioStream = lib.demuxer(file).audioStreams().get(0);
		AudioFormat format = audioStream.properties().format();
		int length = (int) (ms * format.getSampleRate() * format.getSampleSizeInBits() * format.getChannels() / 8000);
		byte[] buffer = new byte[length + 16384];
		IAudioFrame frame;
		int offset = 0;
		while ((frame = audioStream.nextFrame())!=null) {
			byte[] chunk = frame.samples();
			System.err.println(" == receiving " + chunk.length/2 + " bytes at offset " + offset/2);
			System.arraycopy(chunk, 0, buffer, offset, chunk.length);
			offset += chunk.length;
		}
		// Assertions.assertEquals(length, offset);
		System.err.println("Audio length: " + offset + "  (diff=" + (offset-length) + ")");
		return Arrays.copyOf(buffer, length);
	}

	protected static File local(String url, String localname) {
		String home = System.getProperty("user.home");
		File file = Paths.get(home, ".velvet-video", "test", localname).toFile();
		file.getParentFile().mkdirs();
		if (!file.exists()) {
			try (InputStream in = new URL(url).openStream()) {
				Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return file;
 	}
}
