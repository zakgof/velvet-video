package com.zakgof.velvetvideo.example;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.zakgof.velvetvideo.IDemuxer;
import com.zakgof.velvetvideo.IMuxer;
import com.zakgof.velvetvideo.IVelvetVideoLib;
import com.zakgof.velvetvideo.IVideoDecoderStream;
import com.zakgof.velvetvideo.IVideoEncoderStream;
import com.zakgof.velvetvideo.IVideoFrame;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;

public class ImagesToVideoAndBack {

	public static void main(String[] args) throws IOException {
		File src = Util.getFile("https://www.sample-videos.com/video123/mkv/240/big_buck_bunny_240p_10mb.mkv", "source.mkv");
		List<BufferedImage> frames = extractFrameImagesFromVideo(src, 10);
		createVideoFromFrameImages(frames, new File(src.getParentFile(), "output.webm"));
	}

	private static List<BufferedImage> extractFrameImagesFromVideo(File src, int framecount) throws IOException {
		List<BufferedImage> images = new ArrayList<>();
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();
		try (IDemuxer demuxer = lib.demuxer(src)) {
			IVideoDecoderStream videoStream = demuxer.videoStreams().get(0);
			for (int i = 0; i < framecount; i++) {
				IVideoFrame videoFrame = videoStream.nextFrame();
				BufferedImage image = videoFrame.image();
				images.add(image);
				File outputFile = new File(src.getParentFile(), "frame-" + i + ".png");
				ImageIO.write(image, "png", outputFile);
				System.out.println(outputFile);
			}
		}
		return images;
	}

	private static void createVideoFromFrameImages(List<BufferedImage> images, File output) {
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();
		try (IMuxer muxer = lib.muxer("webm").videoEncoder(lib.videoEncoder("libvpx-vp9").framerate(1, 5))
				.build(output)) {
			IVideoEncoderStream encoder = muxer.videoEncoder(0);
			for (BufferedImage image : images) {
				encoder.encode(image);
			}
		}
		System.out.println(output);
	}

}
