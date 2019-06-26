package com.zakgof.velvetvideo;

import java.io.IOException;
import java.io.OutputStream;

import com.zakgof.velvetvideo.IVideoLib.IEncoderStream;

public class OutputStreamEncoderStream implements IEncoderStream {

    private final OutputStream stream;

    OutputStreamEncoderStream(OutputStream stream) {
        this.stream = stream;
    }

    @Override
    public void send(byte[] bytes) {
        try {
            stream.write(bytes);
        } catch (IOException e) {
            throw new VelvetVideoException(e);
        }
    }

    @Override
    public void close() {
        try {
            stream.close();
        } catch (IOException e) {
            throw new VelvetVideoException(e);
        }

    }

}
