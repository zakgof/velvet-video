package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
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

            IMuxer build(File outputFile);
            
            IMuxer build(ISeekableOutput output);
        }

        void close();

        IEncoder videoStream(String name);

    }

    IDecoder decoder(InputStream is);

    interface IDecoder extends AutoCloseable {
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
    
    public static class FileOutput implements ISeekableOutput {
        
        private SeekableByteChannel channel;
        private FileOutputStream fos;

        public FileOutput(SeekableByteChannel channel) {
            
        }

        public FileOutput(FileOutputStream fos) {
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

}
