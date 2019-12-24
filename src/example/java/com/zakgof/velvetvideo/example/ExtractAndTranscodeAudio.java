package com.zakgof.velvetvideo.example;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;

import com.zakgof.velvetvideo.IAudioDecoderStream;
import com.zakgof.velvetvideo.IAudioEncoderStream;
import com.zakgof.velvetvideo.IAudioFrame;
import com.zakgof.velvetvideo.IDecodedPacket;
import com.zakgof.velvetvideo.IDemuxer;
import com.zakgof.velvetvideo.IMuxer;
import com.zakgof.velvetvideo.IVelvetVideoLib;
import com.zakgof.velvetvideo.MediaType;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;

public class ExtractAndTranscodeAudio {

	public static void main(String[] args) {
		File src = Util.getFile("https://www.sample-videos.com/video123/mkv/240/big_buck_bunny_240p_10mb.mkv", "source.mkv");
		extractAndTranscodeFirstAudioTrack(src);
		extractAndTranscodeMultipleAudioTracks(src);
	}

	private static void extractAndTranscodeFirstAudioTrack(File src) {
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();
		try (IDemuxer demuxer = lib.demuxer(src)) {
			IAudioDecoderStream audioStream = demuxer.audioStreams().get(0);
			AudioFormat format = audioStream.properties().format();
			File output = new File(src.getParent(), "extracted_first.mp3");
			System.out.println(output);
			try (IMuxer muxer = lib.muxer("mp3").audioEncoder(lib.audioEncoder("libmp3lame", format)).build(output)) {
				IAudioEncoderStream audioEncoder = muxer.audioEncoder(0);
				for (IAudioFrame audioFrame : audioStream) {
					audioEncoder.encode(audioFrame.samples());
				}
			}
		}
	}

	private static void extractAndTranscodeMultipleAudioTracks(File src) {
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();
		try (IDemuxer demuxer = lib.demuxer(src)) {
			Map<Integer, IMuxer> muxers = new HashMap<>();
			for (IAudioDecoderStream audioStream : demuxer.audioStreams()) {
				AudioFormat format = audioStream.properties().format();
				File output = new File(src.getParent(), "extracted_" + audioStream.index() + ".mp3");
				System.out.println(output);
				IMuxer muxer = lib.muxer("mp3").audioEncoder(lib.audioEncoder("libmp3lame", format)).build(output);
				muxers.put(audioStream.index(), muxer);
			}
			for (IDecodedPacket<?> packet : demuxer) {
				if (packet.is(MediaType.Audio)) {
					IAudioFrame audioFrame = packet.asAudio();
					IMuxer muxer = muxers.get(packet.stream().index());
					muxer.audioEncoder(0).encode(audioFrame.samples());
				}
			}
			for (IMuxer muxer : muxers.values()) {
				muxer.close();
			}
		}
	}

}
