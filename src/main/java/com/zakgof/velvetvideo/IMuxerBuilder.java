package com.zakgof.velvetvideo;

import java.io.File;

public interface IMuxerBuilder {

	IMuxerBuilder videoEncoder(IVideoEncoderBuilder encoderBuilder);

	IMuxerBuilder audioEncoder(IAudioEncoderBuilder encoderBuilder);

	IMuxerBuilder remuxer(IRemuxerBuilder decoder);

	IMuxerBuilder remuxer(IDecoderStream<?, ?, ?> decoder);

	IMuxerBuilder metadata(String key, String value);

	IMuxer build(File outputFile);

	IMuxer build(ISeekableOutput output);
}