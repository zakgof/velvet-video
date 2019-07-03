package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.zakgof.velvetvideo.IVideoLib.IDecoderVideoStream;
import com.zakgof.velvetvideo.IVideoLib.IDemuxer;
import com.zakgof.velvetvideo.IVideoLib.IMuxer;

public class EncodeDecodeTest {
    
    @Test
    public void testEncodeDecode() throws IOException {
        
        IVideoLib lib = new FFMpegVideoLib();
        Path dir = Files.createTempDirectory("velvet-video-test-");
        Path file = dir.resolve("stream.mp4");
        

        BufferedImage[] orig = new BufferedImage[5];
        try (IMuxer muxer = lib.muxer("mp4").videoStream("dflt", 
            lib.encoder("mpeg4").bitrate(3000).framerate(1)).build(file.toFile())) {
            for (int i=0; i<orig.length; i++) {
                BufferedImage image = genImage1(i);
                muxer.videoStream("dflt").encode(image, i);
                orig[i] = image;
            }
        }
        
        double dff = diff(orig[0], orig[3]);
        System.err.println("[0] to [3] " + dff);
        try (FileInputStream fis = new FileInputStream(file.toFile()); 
                IDemuxer decoder = lib.demuxer(fis)) {
            
            IDecoderVideoStream stream = decoder.videoStreams().get(0);
            for (int i=0; i<orig.length; i++) {
                BufferedImage imgrestored = stream.nextFrame();
                ImageIO.write(orig[i], "png", new File("c:\\pr\\orig-" + i + ".png"));
                ImageIO.write(imgrestored, "png", new File("c:\\pr\\rest-" + i + ".png"));
                double diff = diff(orig[i], imgrestored);
                System.err.println(diff);
                Assertions.assertEquals(0, diff, 20.0, "Frame " + i + " differs by " + diff);
            }   
            
            BufferedImage imgrestored = stream.nextFrame();
            Assertions.assertNull(imgrestored);
        }
    }

    private double diff(BufferedImage im1, BufferedImage im2) {
        byte[] bytes1 = ((DataBufferByte) im1.getRaster().getDataBuffer()).getData();
        byte[] bytes2 = ((DataBufferByte) im2.getRaster().getDataBuffer()).getData();
        double diff = 0;
        for (int s=0; s<bytes1.length; s++) {
            double delta = bytes1[s] - bytes2[s];
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
                bytes[offset] =     (byte) ((int) (127 + 127 * Math.sin(x * 0.12  / (seed+1))) & 0xFF);
                bytes[offset + 1] = (byte) ((int) (127 + 127 * Math.sin(y * 0.081  / (seed+1))) & 0xFF);
                bytes[offset + 2] = (byte) ((int) (127 + 127 * Math.sin((x+y) * 0.01 / (seed+1))) & 0xFF);
            }
        }
        return image;
    }

}
