package com.zakgof.velvetvideo;

/**
 * Muxer - interface to write media streams into a media container.
 */
public interface IMuxer extends AutoCloseable {

	/**
	 * Get a video encoding media stream earlier added to this muxer. Throws an exception if the stream with such index does not exist or is not a video encoding stream.
	 * @param index stream index.
	 * @return encoder stream instance
	 */
	IVideoEncoderStream videoEncoder(int index);

	/**
	 * Get an audio encoding media stream earlier added to this muxer. Throws an exception if the stream with such index does not exist or is not an audio encoding stream.
	 * @param index stream index.
	 * @return encoder stream instance
	 */
	IAudioEncoderStream audioEncoder(int index);

	/**
	 * Get a remuxer media stream earlier added to this muxer. Throws an exception if the stream with such index does not exist or is not a remuxing stream.
	 * @param index stream index.
	 * @return remuxer stream instance
	 */
	IRemuxerStream remuxer(int index);

	/**
	 * Free resources and close the muxer. It is necessary to call this method to avoid resource leaking.
	 */
	@Override
	void close();

}