package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface IVideoLib {

    List<String> codecs(Direction dir);

    IEncoder.IBuilder encoder(String format);

    IMuxer.IBuilder muxer(String format);

    interface IEncoder {

        void encode(BufferedImage bi, long pts);

        void writeRaw(byte[] packetData);

        interface IBuilder {
            IBuilder framerate(int framerate);

            IBuilder bitrate(int bitrate);

            IBuilder param(String key, String value);

            IBuilder metadata(String key, String value);

            IBuilder enableExperimental();

        }

    }

    interface IEncoderStream extends AutoCloseable {
        void send(byte[] bytes);

        @Override
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

        @Override
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

    interface IDemuxer extends AutoCloseable, Iterable<IDecodedPacket> {
        @Override
		void close();
        List<? extends IDecoderVideoStream> videos();

        @Deprecated
        boolean nextPacket(Consumer<IFrame> videoConsumer, Consumer<IAudioPacket> audioConsumer);

        // TODO: iterator or stream !!
        IDecodedPacket nextPacket();

        Map<String, String> metadata();
        IMuxerProperties properties();

        Stream<IDecodedPacket> stream();

        @Override
        Iterator<IDecodedPacket> iterator();

		IDecoderVideoStream videoStream(String streamName);
    }

    interface IDecodedPacket {
    	default IFrame video() {
    		return null;
    	}
    	default boolean isVideo() {
    		return false;
    	}
    	// boolean isAudio();
    	// TODO: enum ?
    }

    interface IDecoderVideoStream {
        String name();

        IFrame nextFrame();

        Map<String, String> metadata();
        IVideoStreamProperties properties();

        IDecoderVideoStream seek(long frame);

		byte[] nextRawPacket();
    }

    interface IFrame {
        IDecoderVideoStream stream();
        BufferedImage image();
        long nanostamp();
    }

    interface IMuxerProperties {
        String format();
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
        @Override
		void close();
        long size();
    }


    interface ISeekableOutput extends AutoCloseable {
        void write(byte[] bytes);
        void seek(long position);
        @Override
		void close();
    }

    enum Direction {
        Encode,
        Decode,
        All
    }

}
