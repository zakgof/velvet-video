package com.zakgof.velvetvideo;

/** Input stream with capability of seeking. */
public interface ISeekableOutput extends AutoCloseable {

	/**
     * Writes bytes from bytes array into the output stream.
     *
     * @param bytes buffer to write data from
     * @throws VelvetVideoException if IO error occurs
     */
    void write(byte[] bytes);

    /**
     * Seeks the write pointer to a specified position.
     * @param position position to seek in bytes from the stream beginning
     */
    void seek(long position);

    /**
     * Closes the output stream and frees the resources.
     */
    @Override
	void close();
}