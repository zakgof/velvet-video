package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface IVideoLib {

    List<String> codecs();

    IEncoder.IBuilder encoder(String format);

    IMuxer.IBuilder muxer(String format);

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
            
            IMuxer.IBuilder video(String name, IEncoder.IBuilder encoderBuilder);

            IMuxer build(File outputFile);
            
            IMuxer build(ISeekableOutput output);

            // IEncoder.IBuilder video(String codec);
        }

        void close();

        IEncoder videoStream(String name);

    }

    IDemuxer demuxer(InputStream is);

    interface IDemuxer extends AutoCloseable {
        void close();

        List<IDecoderVideoStream> videoStreams();
    }

    interface IDecoderVideoStream extends AutoCloseable {
        void close();

        BufferedImage nextFrame();
    }
    
    interface ISeekableOutput extends AutoCloseable {
        void write(byte[] bytes);
        void seek(long position);
        void close();
    }
    
    interface IVideoProperties {
        String codec();
        double framerate();
        long duration();
        long frames();
        int width();
        int height();
    }
    
    interface ISeekableInput extends AutoCloseable {
        int read(byte[] bytes);
        void seek(long position);
        void close();
        long size();
    }

}
