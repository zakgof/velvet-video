package com.zakgof.velvetvideo;

public interface ISeekableInput extends AutoCloseable {
    int read(byte[] bytes);
    void seek(long position);
    @Override
	void close();
    long size();
}