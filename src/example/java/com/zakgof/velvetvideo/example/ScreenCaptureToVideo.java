package com.zakgof.velvetvideo.example;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;

import com.zakgof.velvetvideo.IMuxer;
import com.zakgof.velvetvideo.IVelvetVideoLib;
import com.zakgof.velvetvideo.IVideoEncoderBuilder;
import com.zakgof.velvetvideo.IVideoEncoderStream;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;

public class ScreenCaptureToVideo {

	private static final int FRAMERATE = 25;

	public static void main(String[] args) throws AWTException {
		File dest = new File(Util.workDir(), "screenCapture.mp4");
		screenCapture(dest, 250);
	}

	private static void screenCapture(File dest, int frames) throws AWTException {

		Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		Robot robot = new Robot();
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();

		IVideoEncoderBuilder encoderBuilder = lib.videoEncoder("libx264").framerate(FRAMERATE)
				.dimensions(screenRect.width, screenRect.height).bitrate(1000000);

		try (IMuxer muxer = lib.muxer("mp4").videoEncoder(encoderBuilder).build(dest)) {
			IVideoEncoderStream videoEncoder = muxer.videoEncoder(0);
			for (int i = 0; i < frames; i++) {
				BufferedImage image = robot.createScreenCapture(screenRect);
				videoEncoder.encode(image);
			}
		}
		System.out.println(dest);
	}

}
