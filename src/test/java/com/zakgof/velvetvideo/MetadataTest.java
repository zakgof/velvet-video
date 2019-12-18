package com.zakgof.velvetvideo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class MetadataTest extends VelvetVideoTest {

    @Test
    public void testStreamMetadata() throws IOException {
        Path file = dir.resolve("stream-metadata.mp4");
        System.err.println(file);
        try (IMuxer muxer = lib.muxer("mp4").videoEncoder(lib.videoEncoder("mpeg4")
            .metadata("language", "ukr")
            .metadata("handler_name", "Track 4"))
            .build(file.toFile())) {
            muxer.videoEncoder(0).encode(colorImage(2));
        }
        try (IDemuxer demuxer = lib.demuxer(file.toFile())) {
            Map<String, String> restored = demuxer.videoStreams().get(0).metadata();
            Assertions.assertEquals("ukr", restored.get("language"));
            Assertions.assertEquals("Track 4", restored.get("handler_name"));
        }
    }

    @Test
    public void testMuxerMetadata() throws IOException {
        Path file = dir.resolve("muxer-metadata.mp4");
        System.err.println(file);
        try (IMuxer muxer = lib.muxer("mp4").videoEncoder(lib.videoEncoder("mpeg4"))
                .metadata("title", "somemp4video")
                .metadata("genre", "drama")
            .build(file.toFile())) {
            muxer.videoEncoder(0).encode(colorImage(2));
        }
        try (IDemuxer demuxer = lib.demuxer(file.toFile())) {
            Map<String, String> restored = demuxer.metadata();
            Assertions.assertEquals("somemp4video", restored.get("title"));
            Assertions.assertEquals("drama", restored.get("genre"));
        }
    }

    @ParameterizedTest
    @CsvSource({

    	  "ffv1,         avi",
//        "flv,          flv", // FAIL: frames=0
          "h263p,        avi",
          "mjpeg,        avi",

          "msmpeg4,      avi",
          "msmpeg4v2,    avi",
  //      "msmpeg4v2,    matroska", // FAIL: frames=0

          "libx264,      mp4",
  //      "libx264,      avi",      // FAIL: no pts data
          "libx264,      mov",
  //      "libx264,      matroska", // FAIL - Invalid data

  //      "libopenh264,  avi",      // FAIL: no pts data
          "libopenh264,  mov",
          "libopenh264,  mp4",
  //      "libopenh264,  matroska", // FAIL - Invalid data

          "libx265,      mp4",
  //      "libx265,      matroska", // FAIL - frames=0
          "wmv1,         avi",
          "wmv2,         avi",
          "mjpeg,        avi",
 //       "mpeg1video,   avi",      // FAIL: no pts data
 //       "mpeg2video,   avi",      // FAIL: no pts data
          "mpeg4,        mp4",
 //        "mpeg4,        matroska",

       //   "libvpx,        webm",      // FAIL: frames=0
       //   "libvpx-vp9,    webm",      // FAIL: frames=0
       //   "libvpx,        ogg",       // FAIL: frames=0
       //   "libvpx,        matroska",  // FAIL: frames=0
       //   "libvpx-vp9,    matroska",  // FAIL: frames=0
    })
    public void testVideoStreamProperties(String codec, String format) throws IOException {
    	int FRAMES = 5;
        Path file = dir.resolve("stream-metadata-" + codec + "." + format);
        System.err.println("Writing " + file);
        try (IMuxer muxer = lib.muxer(format).videoEncoder(lib.videoEncoder(codec)
        		.framerate(25)
        		.dimensions(640, 480))
        	.build(file.toFile())) {
            IVideoEncoderStream encoder = muxer.videoEncoder(0);
            for (int i=0; i<FRAMES; i++) {
            	encoder.encode(colorImage(i));
            }
        }
        try (IDemuxer demuxer = lib.demuxer(file.toFile())) {
            IVideoDecoderStream videoStream = demuxer.videoStreams().get(0);
			IVideoStreamProperties restored = videoStream.properties();
			for (int i=0; i<FRAMES; i++) {
	            IVideoFrame frame = videoStream.nextFrame();
	            Assertions.assertEquals(40000000L, frame.nanoduration());
	            Assertions.assertEquals(i * 40000000L, frame.nanostamp());
			}

            // Assertions.assertEquals(codec, restored.codec()); TODO: fix codec/encodingformat mess
            Assertions.assertEquals(FRAMES, restored.frames());
            Assertions.assertEquals(40L * FRAMES * 1000000L, restored.nanoduration());
            Assertions.assertEquals(640, restored.width());
            Assertions.assertEquals(480, restored.height());
            Assertions.assertEquals(25.0, restored.framerate(), 0.01);
        }
    }


}
