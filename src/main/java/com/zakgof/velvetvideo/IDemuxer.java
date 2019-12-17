package com.zakgof.velvetvideo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface IDemuxer extends AutoCloseable, Iterable<IDecodedPacket<?>> {
	@Override
	void close();

	List<? extends IDecoderVideoStream> videoStreams();

	IDecoderVideoStream videoStream(int index);

	List<? extends IDecoderAudioStream> audioStreams();

	IDecoderAudioStream audioStream(int index);

	Map<String, String> metadata();

	IMuxerProperties properties();

	IDecodedPacket<?> nextPacket();

	Stream<IDecodedPacket<?>> stream();

	@Override
	Iterator<IDecodedPacket<?>> iterator();
}