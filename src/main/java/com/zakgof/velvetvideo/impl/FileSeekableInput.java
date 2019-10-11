package com.zakgof.velvetvideo.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import com.zakgof.velvetvideo.ISeekableInput;
import com.zakgof.velvetvideo.VelvetVideoException;

public class FileSeekableInput implements ISeekableInput {

    private SeekableByteChannel channel;
    private FileInputStream fos;

    public FileSeekableInput(FileInputStream fis) {
        this.fos = fis;
        this.channel = fis.getChannel();
    }

    @Override
    public int read(byte[] bytes) {
        try {
            return channel.read(ByteBuffer.wrap(bytes));
        } catch (IOException e) {
            throw new VelvetVideoException(e);
        }
    }

    @Override
    public void seek(long position) {
        try {
            channel.position(position);
        } catch (IOException e) {
            throw new VelvetVideoException(e);
        }
    }

    @Override
    public void close() {
        try {
            channel.close();
            fos.close();
        } catch (IOException e) {
            throw new VelvetVideoException(e);
        }
    }

    @Override
    public long size() {
        try {
            return channel.size();
        } catch (IOException e) {
            throw new VelvetVideoException(e);
        }
    }

}