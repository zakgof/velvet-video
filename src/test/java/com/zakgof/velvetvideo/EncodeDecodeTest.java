package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.zakgof.velvetvideo.IVideoLib.IDemuxer;
import com.zakgof.velvetvideo.IVideoLib.IDecoderVideoStream;
import com.zakgof.velvetvideo.IVideoLib.IEncoder;

public class EncodeDecodeTest {
    
    @Test
    public void testEncodeDecode() throws IOException {
        
        IVideoLib lib = new FFMpegVideoLib();
        Path dir = Files.createTempDirectory("velvet-video-test-");
        Path file = dir.resolve("stream");

        BufferedImage[] orig = new BufferedImage[5];
        try (IEncoder encoder = lib.encoder("libx264")
           .bitrate(400000)
           .build(new FileOutputStream(file.toFile(), false))) {
        
            for (int i=0; i<orig.length; i++) {
                BufferedImage image = genImage1(i);
                encoder.encode(image, i);
            }
        }
        try (FileInputStream fis = new FileInputStream(file.toFile()); 
                IDemuxer decoder = lib.demuxer(fis)) {
            
            IDecoderVideoStream stream = decoder.videoStreams().get(0);
            for (int i=0; i<orig.length; i++) {
                BufferedImage imgrestored = stream.nextFrame();
                double diff = diff(orig[i], imgrestored);
                Assertions.assertEquals(0, diff, 1.0, "Frame " + i + " differs by " + diff);
            }        
        }
    }

    private double diff(BufferedImage im1, BufferedImage im2) {
        byte[] bytes1 = ((DataBufferByte) im1.getRaster().getDataBuffer()).getData();
        byte[] bytes2 = ((DataBufferByte) im2.getRaster().getDataBuffer()).getData();
        double diff = 0;
        for (int s=0; s<bytes1.length; s++) {
            double delta = bytes1[s] - bytes2[3];
            diff += Math.sqrt(delta * delta);
        }
        return diff / bytes1.length;
    }

    private BufferedImage genImage1(int seed) {
        Random random = new Random(seed);
        double offset1 = random.nextDouble();
        double offset2 = random.nextDouble();
        double offset3 = random.nextDouble();
        
        BufferedImage image = new BufferedImage(640,  480, BufferedImage.TYPE_3BYTE_BGR);
        DataBufferByte dataBuffer = (DataBufferByte) image.getRaster().getDataBuffer();
        byte[] bytes = dataBuffer.getData();
        for (int x=0; x<640; x++) {
            for (int y=0; y<480; y++) {
                int offset = (x + y * 640) * 3; 
                bytes[offset] = (byte) (127 * Math.sin(x * 0.1  + offset1));
                bytes[offset + 1] = (byte) (127 * Math.sin(x * 0.14  + offset2));
                bytes[offset + 2] = (byte) (127 * Math.sin(y * 0.1  + offset3));
            }
        }
        return image;
    }

}
