package com.zakgof.velvetvideo;

public interface IVideoStreamProperties {
    String codec();
    double framerate();
    long nanoduration();
    long frames();
    int width();
    int height();
}