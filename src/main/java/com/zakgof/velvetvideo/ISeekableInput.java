package com.zakgof.velvetvideo;

/** Input stream with capability of seeking. */
public interface ISeekableInput extends AutoCloseable {

    /**
     * Read bytes from the input stream into the bytes array.
     *
     * @param bytes buffer to read data to
     * @return number of bytes read.
     * @throws VelvetVideoException if IO error occurs
     */
    int read(byte[] bytes);

    /**
     * Seek read pointer to the specified position.
     * @param position offset from the stream start to put the read pointer
     */
    void seek(long position);

    /**
     * @return total number of bytes in this stream
     */
    long size();

    /**
     * Close the input stream and free the resources.
     */
    @Override
	void close();
}