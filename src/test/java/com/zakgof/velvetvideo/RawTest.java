package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.zakgof.velvetvideo.IVideoLib.IDecodedPacket;
import com.zakgof.velvetvideo.IVideoLib.IDecoderVideoStream;
import com.zakgof.velvetvideo.IVideoLib.IDemuxer;
import com.zakgof.velvetvideo.IVideoLib.IMuxer;
import com.zakgof.velvetvideo.IVideoLib.IVideoStreamProperties;

public class RawTest extends VelvetVideoTest {

	private static final int FRAMES = 10;

	@Test
	public void testRemux() throws IOException {
		File file = dir.resolve("orig.avi").toFile();
		File remuxed = dir.resolve("remuxed.mp4").toFile();
		System.err.println(file + "->" + remuxed);

		// Create and read original AVI
		createSingleStreamVideo("libx264", "avi", file, FRAMES);
		List<BufferedImage> rest1 = loadFrames(file, FRAMES);

		// Remux raw stream to MP4
		try (IDemuxer demuxer = lib.demuxer(file)) {
			IDecoderVideoStream origStream = demuxer.videoStream("video0"); // TODO: this is bug: stream names are not saved to AVIs
			try (IMuxer muxer = lib.muxer("mp4").video("dflt", lib.encoder("libx264")).build(remuxed)) {
				byte[] rawPacket;
				while ((rawPacket = origStream.nextRawPacket()) != null) {
					muxer.video("dflt").writeRaw(rawPacket);
				}
			}
		}

		// Read and check MP4 frames
		try (IDemuxer demuxer = lib.demuxer(remuxed)) {
			for (int i=0; i<FRAMES; i++) {
				IDecodedPacket packet = demuxer.nextPacket();
				BufferedImage remuxedImage = packet.video().image();
				assertEqual(remuxedImage, rest1.get(i));
			}
		}
	}

	@Test
	public void testMergeMp4s() throws IOException {
		File file = dir.resolve("orig.mp4").toFile();
		File remuxed = dir.resolve("triple.mp4").toFile();
		System.err.println(file + "->" + remuxed);

		// Create and read original MP4
		createSingleStreamVideo("libx264", "mp4", file, FRAMES);
		List<BufferedImage> original = loadFrames(file, FRAMES);

		// Remux raw streams
		int TIMES = 4;
		try (IMuxer muxer = lib.muxer("mp4")
		    .video("dflt", lib.encoder("libx264"))
		    .build(remuxed)) {
			for (int t=0; t<TIMES; t++) {
				try (IDemuxer demuxer = lib.demuxer(file)) {
					IDecoderVideoStream origStream = demuxer.videos().get(0);
					byte[] rawPacket;
					while ((rawPacket = origStream.nextRawPacket()) != null) {
						muxer.video("dflt").writeRaw(rawPacket);
					}
				}
			}
		}

		// Read and check MP4 frames
		try (IDemuxer demuxer = lib.demuxer(remuxed)) {
			for (int t=0; t<TIMES; t++) {
				for (int i=0; i<FRAMES; i++) {
					IDecodedPacket packet = demuxer.nextPacket();
					BufferedImage remuxedImage = packet.video().image();
					assertEqual(remuxedImage, original.get(i));
				}
			}
			IVideoStreamProperties mergedStreamProperties = demuxer.videoStream("dflt").properties();
			IVideoStreamProperties originalStreamProperties = lib.demuxer(file).videoStream("dflt").properties();
			Assertions.assertEquals(originalStreamProperties.duration() * TIMES, mergedStreamProperties.duration(), 1.0);
			Assertions.assertEquals(originalStreamProperties.frames() * TIMES, mergedStreamProperties.frames());
			Assertions.assertEquals(originalStreamProperties.framerate(), mergedStreamProperties.framerate(), 0.001);
			Assertions.assertEquals(originalStreamProperties.height(), mergedStreamProperties.height());
		}
	}
}
