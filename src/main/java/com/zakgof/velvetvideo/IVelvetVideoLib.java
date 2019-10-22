package com.zakgof.velvetvideo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import javax.sound.sampled.AudioFormat;

public interface IVelvetVideoLib {

	List<String> codecs(Direction dir, MediaType mediaType);

	IVideoEncoderBuilder videoEncoder(String codec);

	IAudioEncoderBuilder audioEncoder(String codec, AudioFormat audioFormat);

	IRemuxerBuilder remuxer(IDecoderStream<?, ?, ?> decoder);

	IMuxerBuilder muxer(String format);

	IDemuxer demuxer(InputStream is);

	default IDemuxer demuxer(File file) {
		try {
			return demuxer(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			throw new VelvetVideoException(e);
		}
	}



}
