package com.zakgof.velvetvideo;

public interface IVideoEncoderBuilder {
    IVideoEncoderBuilder framerate(int framerate);

    IVideoEncoderBuilder framerate(int num, int den);

    IVideoEncoderBuilder bitrate(int bitrate);

    IVideoEncoderBuilder dimensions(int width, int height);

    IVideoEncoderBuilder param(String key, String value);

    IVideoEncoderBuilder metadata(String key, String value);

    IVideoEncoderBuilder enableExperimental();

    IVideoEncoderBuilder filter(String filter);

}