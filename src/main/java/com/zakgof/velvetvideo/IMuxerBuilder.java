package com.zakgof.velvetvideo;

import java.io.File;

public interface IMuxerBuilder {

    IMuxerBuilder video(IEncoderBuilder encoderBuilder);
    IMuxerBuilder video(IDecoderVideoStream decoder);

    IMuxerBuilder metadata(String key, String value);

    IMuxer build(File outputFile);

    IMuxer build(ISeekableOutput output);
}