package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface IVideoLib {

    List<String> codecs();

    IEncoder.IBuilder encoder(String format);

    IMuxer.IBuilder muxer(String format);

    interface IEncoder {

        void encode(BufferedImage bi, long pts);

        interface IBuilder {
            IBuilder framerate(int framerate);

            IBuilder bitrate(int bitrate);

            IBuilder param(String key, String value);
            
            IBuilder metadata(String key, String value);

            // IEncoder build(OutputStream output);
        }

    }

    interface IEncoderStream extends AutoCloseable {
        void send(byte[] bytes);

        void close();
    }

    interface IMuxer extends AutoCloseable {

        interface IBuilder {
            
            IMuxer.IBuilder video(String name, IEncoder.IBuilder encoderBuilder);
            
            IBuilder metadata(String key, String value);

            IMuxer build(File outputFile);
            
            IMuxer build(ISeekableOutput output);

            // IEncoder.IBuilder video(String codec);
        }

        void close();

        IEncoder video(String name);

    }

    IDemuxer demuxer(InputStream is);
    
    default IDemuxer demuxer(File file) {
        try {
            return demuxer(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new VelvetVideoException(e);
        }
    }

    interface IDemuxer extends AutoCloseable {
        void close();
        List<? extends IDecoderVideoStream> videos();
        boolean nextPacket(Consumer<IFrame> videoConsumer, Consumer<IAudioPacket> audioConsumer);
        Map<String, String> metadata(); 
    }

    interface IDecoderVideoStream {
        String name();
        // void close();
        // IFrame nextFrame();

        Map<String, String> metadata();
        IVideoStreamProperties properties();
    }
    
    interface IFrame {
        IDecoderVideoStream stream();
        BufferedImage image();
        long nanostamp();
    }

    
    interface IVideoStreamProperties {
        String codec();
        double framerate();
        long duration();
        long frames();
        int width();
        int height();
    }
    
    interface IAudioPacket {
        // IAudioStream stream();
        byte[] data();
    }
    
    interface ISeekableInput extends AutoCloseable {
        int read(byte[] bytes);
        void seek(long position);
        void close();
        long size();
    }
    
    
    interface ISeekableOutput extends AutoCloseable {
        void write(byte[] bytes);
        void seek(long position);
        void close();
    }

    

}
