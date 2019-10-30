package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class VarTimingTest extends VelvetVideoTest {

	private static final int FRAMES = 20;

	@ParameterizedTest
    @CsvSource({

         "ffv1,         avi",
         "flv,          flv",    // Dur 8.0 (expected 8.4),  zero avstream duration
         "h263p,        avi",
         "mjpeg,        avi",

         "msmpeg4,      avi",
         "msmpeg4v2,    avi",
         "msmpeg4v2,    matroska", // zero avstream duration

   //    "libx264,      mp4",      // Dur 7.0 (expected 8.40), Missing frames
   //    "libx264,      mov",      // Dur 7.0 (expected 8.40), Missing frames
   //    "libx264,      avi",      // No PTS data in either stream or container
   //    "libx264,      matroska", // FAIL - Invalid data

   //    "libopenh264,  avi",        // [FILE OK] No PTS data in either stream or container
         "libopenh264,  mov",
         "libopenh264,  mp4",
   //    "libopenh264,  matroska",   // FAIL - Invalid data

  //     "libx265,      mp4",        // Dur 7.0 (expected 8.40), Missing frames
         "libx265,      matroska",   // zero avstream duration
         "wmv1,         avi",
   //    "wmv2,         avi",        // Mess in content, duration OK
         "mjpeg,        avi",
    //   "mpeg1video,   avi",        // Dur 7.68 (expected 2.00), No PTS data in either stream or container
    //   "mpeg2video,   avi",        // Dur 7.68 (expected 2.00), No PTS data in either stream or container
         "mpeg4,        mp4",
         "libvpx,        webm",      // zero avstream duration
         "libvpx-vp9,    webm",      // zero avstream duration
         "libvpx,        ogg",
         "libvpx,        matroska",  // zero avstream duration
         "libvpx-vp9,    matroska",  // zero avstream duration
    })
	public void testVarTiming(String codec, String format) throws IOException {
		File file = file("vartime-" + codec + "." + format);
		System.err.println(file);

		// Write file
		BufferedImage[] orig = new BufferedImage[FRAMES];
		try (IMuxer muxer = lib.muxer(format)
				.videoEncoder(lib.videoEncoder(codec)
					.bitrate(80000000)
					.dimensions(640, 480)
					.framerate(25)
					.enableExperimental())
				.build(file)) {
			for (int i = 0; i < FRAMES; i++) {
				BufferedImage image = colorImage(i);
				muxer.videoEncoder(0).encode(image, (i+1));
				orig[i] = image;
			}
		}

		try (IDemuxer demuxer = lib.demuxer(file)) {
			IVideoStreamProperties properties = demuxer.videoStream(0).properties();
			// Read and check MP4 frames
			for (int i=0; i<FRAMES; i++) {
				IDecodedPacket packet = demuxer.nextPacket();
				Assertions.assertNotNull(packet, "Frame " + i + " n'existe pas");
				IVideoFrame frame = packet.video();
			//	Assertions.assertEquals(40000000L * (i+1), frame.nanoduration());
				Assertions.assertEquals(40000000L * (i+1)*i/2, frame.nanostamp(), "Bad stamp for frame " + i);
				assertEqual(orig[i], frame.image(), 10);
			}
			Assertions.assertEquals(40000000L * (FRAMES+1)*FRAMES/2, demuxer.properties().nanoduration());
			Assertions.assertEquals(40000000L * (FRAMES+1)*FRAMES/2, properties.nanoduration());
		}
	}

}

