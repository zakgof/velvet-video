package com.zakgof.velvetvideo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;


public interface IVelvetVideoLib {

    List<String> codecs(Direction dir);

    IVideoEncoderBuilder videoEncoder(String format);

    IVideoRemuxerBuilder videoRemux(IDecoderVideoStream decoder);

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
