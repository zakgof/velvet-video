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
import com.zakgof.velvetvideo.IVideoLib.IMuxer;

public class RawTest extends VelvetVideoTest {

	private static final int FRAMES = 10;

	@Test
	public void testRemux() throws IOException {
		File file = dir.resolve("orig.avi").toFile();
		File remuxed = dir.resolve("remuxed.mp4").toFile();
		System.err.println(file + "->" + remuxed);

		// Create and read original AVI
		createSingleStreamVideo("libx264", "avi", file, FRAMES);
		List<BufferedImage> rest1 = new ArrayList<>(FRAMES);
		try (IDemuxer demuxer = lib.demuxer(file)) {
			for (IDecodedPacket packet : demuxer) {
				rest1.add(packet.video().image());
			}
		}
		Assertions.assertEquals(FRAMES, rest1.size());

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
				double diff = diff(remuxedImage, rest1.get(i));
				Assertions.assertEquals(0, diff, 1.0);
			}
		}
	}
}
