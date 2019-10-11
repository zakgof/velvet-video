package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;

public interface IEncoderVideoStream {

    void encode(BufferedImage image);

    void encode(BufferedImage image, int duration);

    void writeRaw(byte[] packetData);



}