package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class RawTest extends VelvetVideoTest {

	private static final int FRAMES = 10;

	 @ParameterizedTest
    @CsvSource({

         "ffv1,         avi",
         "flv,          flv",
         "h263p,        avi",
         "mjpeg,        avi",

         "msmpeg4,      avi",
         "msmpeg4v2,    avi",
         "msmpeg4v2,    matroska",

         "libx264,      mp4",
         "libx264,      avi",
         "libx264,      mov",
     //  "libx264,      matroska", // FAIL - Invalid data

         "libopenh264,  avi",
         "libopenh264,  mov",
         "libopenh264,  mp4",
     //  "libopenh264,  matroska", // FAIL - Invalid data

         "libx265,      mp4",
         "libx265,      matroska",
         "wmv1,         avi",
         "wmv2,         avi",
         "mjpeg,        avi",
         "mpeg1video,   avi",
         "mpeg2video,   avi",
         "mpeg4,        mp4",
         "libvpx,        webm",
         "libvpx-vp9,    webm",
         "libvpx,        ogg",
         "libvpx,        matroska",
         "libvpx-vp9,    matroska",
    })
	public void testRemux(String codec, String format) throws IOException {
		File file = dir.resolve("orig-" + codec + "." + format).toFile();
		File remuxed = dir.resolve("remuxed-" + codec + "." + format).toFile();
		System.err.println(file + "->" + remuxed);

		// Create and read original AVI
		createSingleStreamVideo(codec, format, file, FRAMES);
		List<BufferedImage> rest1 = loadFrames(file, FRAMES);

		// Remux raw stream to MP4
		try (IDemuxer demuxer = lib.demuxer(file)) {
			IDecoderVideoStream origStream = demuxer.videoStream(0);
			try (IMuxer muxer = lib.muxer(format).remuxer(origStream).build(remuxed)) {
				byte[] rawPacket;
				while ((rawPacket = origStream.nextRawPacket()) != null) {
					muxer.remuxer(0).writeRaw(rawPacket);
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
		    .remuxer(lib.demuxer(file).videoStream(0))
		    .build(remuxed)) {
			for (int t=0; t<TIMES; t++) {
				try (IDemuxer demuxer = lib.demuxer(file)) {
					IDecoderVideoStream origStream = demuxer.videoStreams().get(0);
					byte[] rawPacket;
					while ((rawPacket = origStream.nextRawPacket()) != null) {
						muxer.remuxer(0).writeRaw(rawPacket);
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
			IVideoStreamProperties mergedStreamProperties = demuxer.videoStream(0).properties();
			IVideoStreamProperties originalStreamProperties = lib.demuxer(file).videoStream(0).properties();
			Assertions.assertEquals(originalStreamProperties.width(), mergedStreamProperties.width());
			Assertions.assertEquals(originalStreamProperties.height(), mergedStreamProperties.height());

			Assertions.assertEquals(originalStreamProperties.frames() * TIMES, mergedStreamProperties.frames());
			Assertions.assertEquals(originalStreamProperties.framerate(), mergedStreamProperties.framerate(), 0.001);

			Assertions.assertEquals(originalStreamProperties.nanoduration() * TIMES, mergedStreamProperties.nanoduration(), 1.0);
		}
	}

	 @ParameterizedTest
	    @CsvSource({

	         "ffv1,         avi",
	     //    "flv,          flv",   TODO: Investigate failure
	         "h263p,        avi",
	         "mjpeg,        avi",

	         "msmpeg4,      avi",
	         "msmpeg4v2,    avi",
	     //  "msmpeg4v2,    matroska", TODO: Investigate failure

	         "libx264,      mp4",
	         "libx264,      avi",
	         "libx264,      mov",
	     //  "libx264,      matroska", // FAIL - Invalid data

	         "libopenh264,  avi",
	         "libopenh264,  mov",
	         "libopenh264,  mp4",
	     //  "libopenh264,  matroska", // FAIL - Invalid data

	         "libx265,      mp4",
  	    //    "libx265,      matroska",     TODO: Investigate failure
	         "wmv1,         avi",
	         "wmv2,         avi",
	         "mjpeg,        avi",
	         "mpeg1video,   avi",
	         "mpeg2video,   avi",
	         "mpeg4,        mp4",
	  //     "libvpx,        webm",  TODO: Investigate failure
	  //     "libvpx-vp9,    webm",
	         "libvpx,        ogg",
	//       "libvpx,        matroska", TODO: Investigate failure
	//       "libvpx-vp9,    matroska",
	    })
		public void testSlowdownWithoutTranscoding(String codec, String format) throws IOException {
			File file = dir.resolve("slowdown-orig-" + codec + "." + format).toFile();
			File remuxed = dir.resolve("slowdown-remuxed-" + codec + "." + format).toFile();
			System.err.println(file + "->" + remuxed);

			// Create and read back the original video file
			createSingleStreamVideo(codec, format, file, FRAMES);
			List<BufferedImage> rest1 = loadFrames(file, FRAMES);

			// Remux raw stream to MP4
			try (IDemuxer demuxer = lib.demuxer(file)) {
				IDecoderVideoStream origStream = demuxer.videoStream(0);
				try (IMuxer muxer = lib.muxer(format).remuxer(lib.remuxer(origStream).framerate(1)).build(remuxed)) {
					byte[] rawPacket;
					while ((rawPacket = origStream.nextRawPacket()) != null) {
						muxer.remuxer(0).writeRaw(rawPacket);
					}
				}
			}

			// Read and check MP4 frames
			try (IDemuxer demuxer = lib.demuxer(remuxed)) {
				IDecoderVideoStream videoStream = demuxer.videoStream(0);
				for (int i=0; i<FRAMES; i++) {
					IVideoFrame frame = videoStream.nextFrame();
					Assertions.assertEquals(1000000000L, frame.nanoduration());
					BufferedImage remuxedImage = frame.image();
					assertEqual(remuxedImage, rest1.get(i));
				}
				Assertions.assertEquals(FRAMES * 1000000000L, videoStream.properties().nanoduration());
			}
		}
}

