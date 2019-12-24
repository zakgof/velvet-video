package com.zakgof.velvetvideo.example;

import java.awt.image.BufferedImage;
import java.io.File;

import com.zakgof.velvetvideo.IDemuxer;
import com.zakgof.velvetvideo.IMuxer;
import com.zakgof.velvetvideo.IVelvetVideoLib;
import com.zakgof.velvetvideo.IVideoDecoderStream;
import com.zakgof.velvetvideo.IVideoEncoderBuilder;
import com.zakgof.velvetvideo.IVideoEncoderStream;
import com.zakgof.velvetvideo.IVideoFrame;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;

public class TranscodeVideoWithTimingEffects {

	public static void main(String[] args) {
		File src = Util.getFile("https://www.sample-videos.com/video123/mkv/240/big_buck_bunny_240p_10mb.mkv", "source.mkv");
		transcodeToVp9WithSloMo(src);
	}

	private static void transcodeToVp9WithSloMo(File src) {
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();
		try (IDemuxer demuxer = lib.demuxer(src)) {
			IVideoDecoderStream videoDecoderStream = demuxer.videoStreams().get(0);
			File output = new File(src.getParent(), "transcodevp9.webm");
			System.out.println(output);
			double origFramerate = videoDecoderStream.properties().framerate();
			// 1/4 framerate
			int newFramerateNum = 4;
			int newFramerateDen = (int) (origFramerate);
			IVideoEncoderBuilder encoderBuilder = lib.videoEncoder("libvpx-vp9")
				.framerate(newFramerateNum, newFramerateDen)
				.bitrate(1000000)
				.dimensions(videoDecoderStream.properties().width(), videoDecoderStream.properties().height());
			try (IMuxer muxer = lib.muxer("webm").videoEncoder(encoderBuilder).build(output)) {
				IVideoEncoderStream videoEncoder = muxer.videoEncoder(0);
				for (IVideoFrame videoFrame : videoDecoderStream) {
					BufferedImage image = videoFrame.image();
					videoEncoder.encode(image);
				}
			}
			System.out.println(output);
		}
	}

}
