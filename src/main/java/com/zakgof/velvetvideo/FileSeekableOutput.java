package com.zakgof.velvetvideo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import com.zakgof.velvetvideo.IVideoLib.ISeekableOutput;

public class FileSeekableOutput implements ISeekableOutput {
    
    private SeekableByteChannel channel;
    private FileOutputStream fos;

    public FileSeekableOutput(SeekableByteChannel channel) {
        
    }

    public FileSeekableOutput(FileOutputStream fos) {
        this.fos = fos;
        this.channel = fos.getChannel();
    }

    @Override
    public void write(byte[] bytes) {
        try {
            channel.write(ByteBuffer.wrap(bytes));
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
    
}