package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.zakgof.velvetvideo.IVideoLib.IDemuxer;
import com.zakgof.velvetvideo.IVideoLib.IMuxer;

public class EncodeDecodeTest extends VelvetVideoTest {
    
    private static final int FRAMES = 16;

    
    @Disabled
    @ParameterizedTest
    @ValueSource(strings = {"mpeg4", "libx264", "libx265"})
    public void testCodeclist(String codec) throws IOException {
        List<String> codecs = lib.codecs();
        Assertions.assertTrue(codecs.contains(codec));
    }

    @ParameterizedTest
    @CsvSource({
        "mpeg4,        avi",
        "libx264,      avi",
        "mpeg2video,   avi",
        "mpeg1video,   avi",
        "mjpeg,        avi",
        "libxvid,      avi",
        
        "mpeg4,        mp4",
        "libx264,      mp4",
        "libx265,      mp4",
        "libvpx-vp9,   mp4",

        "mpeg4,        matroska",
        "libx264,      matroska",
        "mpeg2video,   matroska",
        "mpeg1video,   matroska",
        "mjpeg,        matroska",
        "libxvid,      matroska",
        "libtheora,    matroska",
    })
    public void testEncodeDecodeCompare(String codec, String format) throws IOException {        
        
        
        Path file = dir.resolve(codec + "." + format);
        System.err.println(file);

        BufferedImage[] orig = new BufferedImage[FRAMES];
        try (IMuxer muxer = lib.muxer(format).video("dflt", 
            lib.encoder(codec).bitrate(30000).framerate(30)).build(file.toFile())) {
            for (int i=0; i<orig.length; i++) {
                BufferedImage image = colorImage(i);
                muxer.video("dflt").encode(image, i);
                orig[i] = image;
            }
        }
        
        double dff = diff(orig[0], orig[3]);
        System.err.println("[0] to [3] " + dff);
        try (FileInputStream fis = new FileInputStream(file.toFile()); 
                IDemuxer demuxer = lib.demuxer(fis)) {
            int[] ai = {0};
            while (demuxer.nextPacket(frame -> {
                    int i = ai[0]; 
                    Assertions.assertTrue(i < FRAMES);
                    BufferedImage imgrestored = frame.image();
                    try {
                        ImageIO.write(orig[i], "png", new File("c:\\pr\\orig-" + i + ".png"));
                        ImageIO.write(imgrestored, "png", new File("c:\\pr\\rest-" + i + ".png"));
                    } catch (IOException e) {
                        Assertions.fail(e);
                    }
                    double diff = diff(orig[i], imgrestored);
                    System.err.println("Diff for frame " +  i + " = " + diff);
                    Assertions.assertEquals(0, diff, 20.0, "Frame " + i + " differs by " + diff);
                    ai[0]++;
            }, null));
        }
    }
    
    @Test
    public void testEncodeDecodeTwoStreams() throws IOException {    
    
        Path file = dir.resolve("two.mp4");
        System.err.println(file);

        BufferedImage[] colorOrig = new BufferedImage[FRAMES];
        BufferedImage[] bwOrig = new BufferedImage[FRAMES];
        try (IMuxer muxer = lib.muxer("mp4")
            .video("color", lib.encoder("mpeg4").framerate(1))
            .video("bw", lib.encoder("mpeg4").framerate(1))
            .build(file.toFile())) {
            for (int i=0; i<FRAMES; i++) {
                BufferedImage color = colorImage(i);
                BufferedImage bw = bwImage(i);
                muxer.video("color").encode(color, i);
                muxer.video("bw").encode(bw, i);
                colorOrig[i] = color;
                bwOrig[i] = bw;
            }
        }
        
        try (FileInputStream fis = new FileInputStream(file.toFile()); 
                IDemuxer demuxer = lib.demuxer(fis)) {
            
            int[] index = {0, 0};
            while (demuxer.nextPacket(frame -> {
                if (frame.stream().name().equals("color")) {                    
                    BufferedImage imgrestored = frame.image();
                    double diff = diff(colorOrig[index[0]], imgrestored);
                    Assertions.assertEquals(0, diff, 10.0, "Color frame " + index[0] + " differs by " + diff);
                    index[0]++;
                }   
                if (frame.stream().name().equals("bw")) {                    
                    BufferedImage imgrestored = frame.image();
                    double diff = diff(bwOrig[index[1]], imgrestored);
                    Assertions.assertEquals(0, diff, 10.0, "BW frame " + index[1] + " differs by " + diff);
                    index[1]++;
                }   
            }, null));
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

  

}
