package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.zakgof.velvetvideo.IVideoLib.IDecodedPacket;
import com.zakgof.velvetvideo.IVideoLib.IDecoderVideoStream;
import com.zakgof.velvetvideo.IVideoLib.IDemuxer;
import com.zakgof.velvetvideo.IVideoLib.IFrame;

public class SeekTest extends VelvetVideoTest {

	private static final int FRAMES = 10;

	@Test
	public void testSeek() throws IOException {
		File file = dir.resolve("seek.mp4").toFile();
		System.err.println(file);

		createSingleStreamVideo("libx264", "mp4", file, FRAMES);
		List<BufferedImage> rest1 = new ArrayList<>(FRAMES);
		try (IDemuxer demuxer = lib.demuxer(file)) {
			for (IDecodedPacket packet : demuxer) {
				rest1.add(packet.video().image());
			}
		}

		Assertions.assertEquals(FRAMES, rest1.size());

		try (IDemuxer demuxer = lib.demuxer(file)) {
			readAndVerify(rest1, "dflt", demuxer, 0);
			readAndVerify(rest1, "dflt", demuxer, 1);
			seekAndVerify(rest1, "dflt", demuxer, 6);
			seekAndVerify(rest1, "dflt", demuxer, 9);
			seekAndVerify(rest1, "dflt", demuxer, 2);
			seekAndVerify(rest1, "dflt", demuxer, 5);
			seekAndVerify(rest1, "dflt", demuxer, 0);
		}
	}

	@Test
	public void testSeekInTwoStreamVideo() throws IOException {
		File file = dir.resolve("seek2.mp4").toFile();
		System.err.println(file);

		createTwoStreamVideo(file, FRAMES);
		List<BufferedImage> restcolor = new ArrayList<>(FRAMES);
		List<BufferedImage> restbw = new ArrayList<>(FRAMES);

		try (IDemuxer demuxer = lib.demuxer(file)) {
			for (IDecodedPacket packet : demuxer) {
				String streamName = packet.video().stream().name();
				if (streamName.equals("color")) {
					restcolor.add(packet.video().image());
				} else if (streamName.equals("bw")) {
					restbw.add(packet.video().image());
				}
			}
		}

		Assertions.assertEquals(FRAMES, restcolor.size());
		Assertions.assertEquals(FRAMES, restbw.size());

		try (IDemuxer demuxer = lib.demuxer(file)) {
			readAndVerify(restcolor, "color", demuxer, 0);
			seekAndVerify(restbw, "bw", demuxer, 9);
			seekAndVerify(restcolor, "color", demuxer, 6);
			seekAndVerify(restcolor, "color", demuxer, 9);
			seekAndVerify(restbw, "bw", demuxer, 1);
			seekAndVerify(restcolor, "color", demuxer, 2);
			seekAndVerify(restbw, "bw", demuxer, 1);
			seekAndVerify(restbw, "bw", demuxer, 4);
			seekAndVerify(restcolor, "color", demuxer, 5);
			seekAndVerify(restcolor, "color", demuxer, 0);
			seekAndVerify(restbw, "bw", demuxer, 7);
			seekAndVerify(restbw, "bw", demuxer, 0);
			seekAndVerify(restbw, "bw", demuxer, 3);
		}
	}

	private void seekAndVerify(List<BufferedImage> rest1, String streamName, IDemuxer demuxer, 	int frameNo) {
		IDecoderVideoStream videoStream = demuxer.videoStream(streamName);
		videoStream.seek(frameNo);
		readAndVerify(rest1, streamName, demuxer, frameNo);
	}

	private void readAndVerify(List<BufferedImage> rest1, String streamName, IDemuxer demuxer, int frameNo) {
		IDecoderVideoStream videoStream = demuxer.videoStream(streamName);
		IFrame frame = videoStream.nextFrame();
		BufferedImage restored = frame.image();
		assertEqual(restored, rest1.get(frameNo));
	}

}
