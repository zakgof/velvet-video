package com.zakgof.velvetvideo.example;

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import com.zakgof.velvetvideo.IAudioFrame;
import com.zakgof.velvetvideo.IDecoderAudioStream;
import com.zakgof.velvetvideo.IDemuxer;
import com.zakgof.velvetvideo.IVelvetVideoLib;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;

public class AudioPlayback {

	public static void main(String[] args) throws Exception {
		File src = Util.getFile("https://www.kozco.com/tech/piano2.wav", "piano2.wav");
		playFirstAudioTrack(src);
	}

	private static void playFirstAudioTrack(File src) throws Exception {
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();
		try (IDemuxer demuxer = lib.demuxer(src)) {
			IDecoderAudioStream audioStream = demuxer.audioStreams().get(0);
			AudioFormat format = audioStream.properties().format();
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
			SourceDataLine soundLine = (SourceDataLine) AudioSystem.getLine(info);
			soundLine.open(format);
			soundLine.start();
			for (IAudioFrame audioFrame : audioStream) {
				byte[] samples = audioFrame.samples();
				soundLine.write(samples, 0, samples.length);
			}
		}
	}


}
