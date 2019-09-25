package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.zakgof.velvetvideo.IVideoLib.IDecoderVideoStream;
import com.zakgof.velvetvideo.IVideoLib.IDemuxer;
import com.zakgof.velvetvideo.IVideoLib.IMuxer;

public class SeekTest extends VelvetVideoTest {
    
    private static final int FRAMES = 10;

    @Test
    public void testSeek() throws IOException {
        Path file = dir.resolve("seek.mp4");
        System.err.println(file);

        BufferedImage[] orig = new BufferedImage[FRAMES];
        try (IMuxer muxer = lib.muxer("mp4").video("dflt",
            lib.encoder("libx264").bitrate(3000000).framerate(30)).build(file.toFile())) {
            for (int i=0; i<orig.length; i++) {
                BufferedImage image = colorImage(i);
                muxer.video("dflt").encode(image, i);
                orig[i] = image;
                ImageIO.write(orig[i], "png", new File("c:\\pr\\orig-" + i + ".png"));
            }
        }
        
        List<BufferedImage> rest1 = new ArrayList<>(FRAMES);
        
        try (IDemuxer demuxer = lib.demuxer(file.toFile())) {
            while(demuxer.nextPacket(f ->  {
                    System.err.println("saving as " + rest1.size());
                    try {
                        ImageIO.write(f.image(), "png", new File("c:\\pr\\rest1-" + rest1.size() + ".png"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    rest1.add(f.image());
                }, null));
        }
        
        Assertions.assertEquals(FRAMES, rest1.size());

        try (IDemuxer demuxer = lib.demuxer(file.toFile())) {
            IDecoderVideoStream videoStream = demuxer.videos().get(0);
            readAndVerify(rest1, demuxer, 0);
            readAndVerify(rest1, demuxer, 1);
            seekAndVerify(rest1, demuxer, videoStream, 6);
            seekAndVerify(rest1, demuxer, videoStream, 9);
            seekAndVerify(rest1, demuxer, videoStream, 2);
            seekAndVerify(rest1, demuxer, videoStream, 5);
            seekAndVerify(rest1, demuxer, videoStream, 0);
        }
    }

    private void seekAndVerify(List<BufferedImage> rest1, IDemuxer demuxer, IDecoderVideoStream videoStream, int timestamp) {
        videoStream.seek(timestamp);
        readAndVerify(rest1, demuxer, timestamp);
    }

    private void readAndVerify(List<BufferedImage> rest1, IDemuxer demuxer, int timestamp) {
        boolean hit[] = {false}; 
        demuxer.nextPacket(frame -> {
            BufferedImage restored = frame.image();
            double diff = diff(restored, rest1.get(timestamp));
            Assertions.assertEquals(0, diff, 1.0);
            hit[0] = true;
        }, null);
        Assertions.assertTrue(hit[0]);
    }
    


}
