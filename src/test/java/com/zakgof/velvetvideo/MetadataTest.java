package com.zakgof.velvetvideo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.zakgof.velvetvideo.IVideoLib.IDemuxer;
import com.zakgof.velvetvideo.IVideoLib.IEncoder;
import com.zakgof.velvetvideo.IVideoLib.IMuxer;
import com.zakgof.velvetvideo.IVideoLib.IVideoStreamProperties;

public class MetadataTest extends VelvetVideoTest {
    
    @Test
    public void testStreamMetadata() throws IOException {
        Path file = dir.resolve("stream-metadata.mp4");
        System.err.println(file);
        try (IMuxer muxer = lib.muxer("mp4").video("color", lib.encoder("mpeg4")
                .metadata("language", "ukr")
                .metadata("handler_name", "Track 4"))
            .build(file.toFile())) {            
            muxer.video("color").encode(colorImage(2), 0);            
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
    
    @Test
    public void testVideoStreamProperties() throws IOException {
        Path file = dir.resolve("stream-metadata.mp4");
        try (IMuxer muxer = lib.muxer("mp4").video("color", lib.encoder("mpeg4").framerate(25)).build(file.toFile())) {            
            IEncoder encoder = muxer.video("color");
            encoder.encode(colorImage(2), 0);
            encoder.encode(colorImage(3), 1);   
        }
        try (IDemuxer demuxer = lib.demuxer(file.toFile())) {
            IVideoStreamProperties restored = demuxer.videos().get(0).properties();
            Assertions.assertEquals("mpeg4", restored.codec());
            Assertions.assertEquals(25.0, restored.framerate(), 0.01);
            Assertions.assertEquals(2L, restored.frames());
            Assertions.assertEquals(80L, restored.duration());
            Assertions.assertEquals(640, restored.width());
            Assertions.assertEquals(480, restored.height());
        }
    }


}
