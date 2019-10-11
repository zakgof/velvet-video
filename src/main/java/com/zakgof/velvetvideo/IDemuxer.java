package com.zakgof.velvetvideo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface IDemuxer extends AutoCloseable, Iterable<IDecodedPacket> {
    @Override
	void close();
    List<? extends IDecoderVideoStream> videos();

    IDecodedPacket nextPacket();

    Map<String, String> metadata();
    IMuxerProperties properties();

    Stream<IDecodedPacket> stream();

    @Override
    Iterator<IDecodedPacket> iterator();

	IDecoderVideoStream video(int index);
}