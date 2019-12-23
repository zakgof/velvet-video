package com.zakgof.velvetvideo.example;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zakgof.velvetvideo.IAudioDecoderStream;
import com.zakgof.velvetvideo.IDecoderStream;
import com.zakgof.velvetvideo.IDemuxer;
import com.zakgof.velvetvideo.IMuxer;
import com.zakgof.velvetvideo.IMuxerBuilder;
import com.zakgof.velvetvideo.IRawPacket;
import com.zakgof.velvetvideo.IRemuxerBuilder;
import com.zakgof.velvetvideo.IVelvetVideoLib;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;

public class RemuxVideo {

	public static void main(String[] args) {
		File src = Util.getFile("https://www.sample-videos.com/video123/mkv/240/big_buck_bunny_240p_10mb.mkv", "source.mkv");
		remuxToMp4(src);
	}

	private static void remuxToMp4(File src) {
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();
		File output = new File(src.getParent(), "remux.mp4");
		System.out.println(output);
		try (IDemuxer demuxer = lib.demuxer(src)) {

			List<IDecoderStream<?,?,?>> decoders = new ArrayList<>();
			decoders.addAll(demuxer.videoStreams());
			decoders.addAll(demuxer.audioStreams());

			IAudioDecoderStream audioStream = demuxer.audioStream(1);

			IRawPacket raw = null;
			while ((raw = audioStream.nextRawPacket()) != null) {
				System.err.println(raw);
			}


			Map<Integer, Integer> decoderToRemuxerIndex = new HashMap<>();
			IMuxerBuilder muxerBuilder = lib.muxer("mp4");
			int remuxerIndex = 0;
			for (IDecoderStream<?,?,?> decoder : decoders) {
				IRemuxerBuilder remuxer = lib.remuxer(decoder);
				muxerBuilder.remuxer(remuxer);
				decoderToRemuxerIndex.put(decoder.index(), remuxerIndex);
				remuxerIndex++;
			}
			try (IMuxer muxer = muxerBuilder.build(output)) {
				IRawPacket rawPacket = null;
				while((rawPacket = demuxer.nextRawPacket()) != null) {
					Integer remuxIndex = decoderToRemuxerIndex.get(rawPacket.streamIndex());
					if (remuxIndex != null) {
						muxer.remuxer(remuxIndex).writeRaw(rawPacket);
					}
				}
			}
			System.out.println(output);
		}
	}

}
