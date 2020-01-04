package com.zakgof.velvetvideo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Demuxer - media container reader.
 */
public interface IDemuxer extends AutoCloseable, Iterable<IDecodedPacket<?>> {

	/**
	 * @return list of container's media streams
	 */
	List<IDecoderStream<?, ?, ?>> streams();

	/**
	 * @return list of container's video streams
	 */
	List<? extends IVideoDecoderStream> videoStreams();

	/**
	 * @return list of container's audio streams
	 */
	List<? extends IAudioDecoderStream> audioStreams();

	/**
	 * @param index of the stream in the container
	 * @return container's video stream by index, or null if a stream with the specified index does not exist or is not video.
	 */
	IVideoDecoderStream videoStream(int index);

	/**
	 * @param index of the stream in the container
	 * @return container's audio stream by index, or null if a stream with the specified index does not exist or is not audio.
	 */
	IAudioDecoderStream audioStream(int index);

	/**
	 * @return stream metadata as key to value map
	 */
	Map<String, String> metadata();

	/**
	 * @return container properties
	 */
	IContainerProperties properties();

	/**
	 * Decodes and returns next packet in the container.
	 * @return next packet or null if end of container reached
	 */
	IDecodedPacket<?> nextPacket();

	/**
	 * @return stream of packets in the container
	 */
	Stream<IDecodedPacket<?>> packetStream();

	/**
	 * @return iterator of packets in the container
	 */
	@Override
	Iterator<IDecodedPacket<?>> iterator();

	/**
	 * Obtains next media packet from the container without decoding it.
	 * @return  raw packet data of null if end of container reached
	 */
	IRawPacket nextRawPacket();

	/**
	 * Closes the demuxer, container file or other handles and frees all the allocated resources. Calling this method is necessary after working with demuxer to avoid resource leaks.
	 */
	@Override
	void close();
}