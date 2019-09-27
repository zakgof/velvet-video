package com.zakgof.velvetvideo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.zakgof.velvetvideo.IVideoLib.IDemuxer;
import com.zakgof.velvetvideo.IVideoLib.IEncoder;
import com.zakgof.velvetvideo.IVideoLib.IMuxer;
import com.zakgof.velvetvideo.IVideoLib.IVideoStreamProperties;

public class MetadataTest extends VelvetVideoTest {

    @Test
    public void testStreamMetadata() throws IOException {
        Path file = dir.resolve("stream-metadata.mp4");
        System.err.println(file);
        try (IMuxer muxer = lib.muxer("mp4").video("Track 4", lib.encoder("mpeg4")
            .metadata("language", "ukr"))
            .build(file.toFile())) {
            muxer.video("Track 4").encode(colorImage(2), 0);
        }
        try (IDemuxer demuxer = lib.demuxer(file.toFile())) {
            Map<String, String> restored = demuxer.videos().get(0).metadata();
            Assertions.assertEquals("ukr", restored.get("language"));
            Assertions.assertEquals("Track 4", restored.get("handler_name"));
        }
    }

    @Test
    public void testMuxerMetadata() throws IOException {
        Path file = dir.resolve("muxer-metadata.mp4");
        System.err.println(file);
        try (IMuxer muxer = lib.muxer("mp4").video("color", lib.encoder("mpeg4"))
                .metadata("title", "somemp4video")
                .metadata("genre", "drama")
            .build(file.toFile())) {
            muxer.video("color").encode(colorImage(2), 0);
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
//        "msmpeg4v2,    matroska", // FAIL: frames=0

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
      //  "wmv2,         avi", // FAIL: restored frame mismatch (!)
          "mjpeg,        avi",
          "mpeg1video,   avi",
          "mpeg2video,   avi",
          "mpeg4,        mp4",
       // "dvvideo,      avi", // FAIL: Need a matching profile

          "libvpx,        webm",
          "libvpx-vp9,    webm",
          "libvpx,        ogg",
          "libvpx,        matroska",
          "libvpx-vp9,    matroska",
    })
    public void testVideoStreamProperties(String codec, String format) throws IOException {
    	int FRAMES = 5;
        Path file = dir.resolve("stream-metadata-" + codec + "." + format);
        System.err.println("Writing " + file);
        try (IMuxer muxer = lib.muxer(format).video("color", lib.encoder(codec).framerate(25)).build(file.toFile())) {
            IEncoder encoder = muxer.video("color");
            for (int i=0; i<FRAMES; i++) {
            	encoder.encode(colorImage(i), i);
            }
        }
        try (IDemuxer demuxer = lib.demuxer(file.toFile())) {
            IVideoStreamProperties restored = demuxer.videos().get(0).properties();
            // Assertions.assertEquals(codec, restored.codec()); TODO: fix codec/encodingformat mess
            Assertions.assertEquals(FRAMES, restored.frames());
            Assertions.assertEquals(40L * FRAMES, restored.duration());
            Assertions.assertEquals(640, restored.width());
            Assertions.assertEquals(480, restored.height());
            Assertions.assertEquals(25.0, restored.framerate(), 0.01);
        }
    }


}
