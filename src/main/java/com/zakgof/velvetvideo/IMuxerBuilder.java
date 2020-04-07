package com.zakgof.velvetvideo;

import java.io.File;

/**
 * Interface to configure and create muxers.
 */
public interface IMuxerBuilder {

	/**
	 * Create a video encoding media stream in this muxer.
	 * @param encoderBuilder video encoder builder instance
	 * @return this builder
	 */
	IMuxerBuilder videoEncoder(IVideoEncoderBuilder encoderBuilder);

	/**
	 * Create an audio encoding media stream in this muxer.
	 * @param encoderBuilder audio encoder builder instance
	 * @return this builder
	 */
	IMuxerBuilder audioEncoder(IAudioEncoderBuilder encoderBuilder);

	/**
	 * Create a remuxer media stream in this muxer for adding raw bitstream data.
	 * @param remuxerBuilder remuxer builder instance
	 * @return this builder
	 */
	IMuxerBuilder remuxer(IRemuxerBuilder remuxerBuilder);

	/**
	 * Create a remuxer media stream in this muxer for adding raw bitstream data from another open media stream.
	 * @param decoder source decoder providing raw bitstream data.
	 * @return this builder
	 */
	IMuxerBuilder remuxer(IDecoderStream<?, ?, ?> decoder);

	/**
	 * Add muxer metadata value for a specific key.
	 * @param key metadata key
	 * @param value metadata value
	 * @return this builder
	 */
	IMuxerBuilder metadata(String key, String value);

	/**
	 * Create a muxer from this builder. Remember to close the muxer by calling {@link IMuxer#close()} after using.
	 * @param output output ISeekableOutput instance
	 * @return muxer instance
	 */
	IMuxer build(ISeekableOutput output);

	/**
	 * Create a muxer from this builder. Remember to close the muxer by calling {@link IMuxer#close()} after using.
	 * @param outputFile output file
	 * @return muxer instance
	 */
	IMuxer build(File outputFile);

}