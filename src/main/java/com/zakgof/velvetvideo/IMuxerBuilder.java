package com.zakgof.velvetvideo;

import java.io.File;

public interface IMuxerBuilder {

	IMuxerBuilder videoEncoder(IVideoEncoderBuilder encoderBuilder);

	IMuxerBuilder videoRemuxer(IVideoRemuxerBuilder decoder);

	IMuxerBuilder videoRemuxer(IDecoderVideoStream decoder);

	IMuxerBuilder metadata(String key, String value);

	IMuxer build(File outputFile);

	IMuxer build(ISeekableOutput output);
}