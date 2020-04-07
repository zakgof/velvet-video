package com.zakgof.velvetvideo;

import java.util.Map;

/**
 * Demuxer stream with audio or video decoder.
 *
 * @param <I> more specific decoder interface, {@link IVideoDecoderStream} or {@link IAudioDecoderStream}
 * @param <F> data frame interface, {@link IVideoFrame} or {@link IAudioFrame}
 * @param <P> stream properties interface, {@link IVideoStreamProperties} or {@link IAudioStreamProperties}
 */
public interface IDecoderStream<I extends IDecoderStream<?, ?, ?>, F, P> extends Iterable<F> {

	/**
	 * @return stream name
	 */
	String name();

	/**
	 * @return next data frame from the stream, or null if EOS
	 */
	F nextFrame();

	/**
	 * @return key-value map of stream metadata
	 */
	Map<String, String> metadata();

	/**
	 * @return stream properties
	 */
	P properties();

	/**
	 * Seek to a specific frame number.
	 * @param frameNumber frame number to seek to
	 * @return this stream
	 */
	I seek(long frameNumber);

	/**
	 * Seek to a specific time.
	 * @param ns time from stream start, in nanoseconds
	 * @return this stream
	 */
	I seekNano(long ns);

	/**
	 * Fetch next raw data packet without decoding it.
	 * @return next raw packet or null if EOS
	 */
	IRawPacket nextRawPacket();

	/**
	 * @return stream index in the container
	 */
	int index();

	/**
	 * Sets a post-decode filter.
	 * @param filterString filter string, see FFMPEG documentation for format
	 */
	// TODO: mutator does not look good here
	void setFilter(String filterString);
}
