package com.zakgof.velvetvideo;

public interface ISeekableOutput extends AutoCloseable {
    void write(byte[] bytes);
    void seek(long position);
    @Override
	void close();
}