package com.zakgof.velvetvideo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface IDemuxer extends AutoCloseable, Iterable<IDecodedPacket<?>> {
	@Override
	void close();

	List<IDecoderStream<?, ?, ?>> streams();

	List<? extends IVideoDecoderStream> videoStreams();
	List<? extends IAudioDecoderStream> audioStreams();

	IVideoDecoderStream videoStream(int index);
	IAudioDecoderStream audioStream(int index);


	Map<String, String> metadata();

	IMuxerProperties properties();

	IDecodedPacket<?> nextPacket();

	Stream<IDecodedPacket<?>> packetStream();

	@Override
	Iterator<IDecodedPacket<?>> iterator();

	IRawPacket nextRawPacket();
}