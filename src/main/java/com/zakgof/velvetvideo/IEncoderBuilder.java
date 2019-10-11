package com.zakgof.velvetvideo;

public interface IEncoderBuilder {
    IEncoderBuilder framerate(int framerate);

    IEncoderBuilder bitrate(int bitrate);

    IEncoderBuilder dimensions(int width, int height);

    IEncoderBuilder param(String key, String value);

    IEncoderBuilder metadata(String key, String value);

    IEncoderBuilder enableExperimental();
    
    IEncoderBuilder filter(String filter);

}