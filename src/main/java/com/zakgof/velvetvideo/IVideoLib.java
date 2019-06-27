package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.List;

public interface IVideoLib {

    List<String> codecs();

    IEncoder.IBuilder encoderBuilder(String format);
    IMuxer.IBuilder muxerBuilder(String format);

    interface IEncoder extends AutoCloseable {

        void encode(BufferedImage bi, long pts);

        void close();

        interface IBuilder {
            IBuilder framerate(int framerate);

            IBuilder bitrate(int bitrate);

            IBuilder param(String key, String value);

            IEncoder build(OutputStream output);
        }

    }

    interface IEncoderStream extends AutoCloseable {
        void send(byte[] bytes);

        void close();
    }

    interface IMuxer extends AutoCloseable {

        interface IBuilder {
            IBuilder videoStream(String name, IEncoder.IBuilder encoderBuilder);
            IMuxer build(OutputStream output);
        }

        void close();

        IEncoder videoStream(String name);
        
    }

}
