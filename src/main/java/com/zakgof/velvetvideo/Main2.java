package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.zakgof.velvetvideo.IVideoLib.IEncoder;
import com.zakgof.velvetvideo.IVideoLib.IMuxer;

public class Main2 {

    public static void main(String[] args) throws IOException {

        IVideoLib lib = new FFMpegVideoLib();

        IEncoder encoder = lib.encoderBuilder("libx264")
            .bitrate(400000)
            .build(new FileOutputStream("c:\\pr\\1.h264", false));
        
        try (IMuxer muxer = lib.muxerBuilder("mp4")
            .videoStream("video1", lib.encoderBuilder("libx264").bitrate(100000))
            .build(new File("c:\\pr\\1.mp4"))) {

            for (int i = 0; i < 300; i++) {
                BufferedImage image = ImageIO.read(new File("C:\\pr\\codeclab\\src\\file-" + i + ".bmp"));
                muxer.videoStream("video1").encode(image, i);
                encoder.encode(image, i);
            }
        }
        
        encoder.close();
       
        
    }

}