package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class SeekTest extends VelvetVideoTest {

	private static final int FRAMES = 10;

    @ParameterizedTest
    @CsvSource({

    	  "ffv1,         avi",
          "flv,          flv",
          "h263p,        avi",
          "mjpeg,        avi",

          "msmpeg4,      avi",
          "msmpeg4v2,    avi",

          "libx264,      mp4",
//        "libx264,      avi",
          "libx264,      mov",

//        "libopenh264,  avi",
          "libopenh264,  mov",
          "libopenh264,  mp4",

          "libx265,      mp4",
          "libx265,      matroska",
          "wmv1,         avi",
          "wmv2,         avi",
          "mjpeg,        avi",
//        "mpeg1video,   avi",
//        "mpeg2video,   avi",
          "mpeg4,        mp4",

          "libvpx,        webm",
          "libvpx-vp9,    webm",
          "libvpx,        ogg",
          "libvpx,        matroska",
          "libvpx-vp9,    matroska",
    })
	public void testSeek(String codec, String format) throws IOException {
    	File file = dir.resolve("seek-" + codec + "." + format).toFile();
		System.err.println(file);

		createSingleStreamVideo(codec, format, file, FRAMES);
		List<BufferedImage> rest1 = new ArrayList<>(FRAMES);
		try (IDemuxer demuxer = lib.demuxer(file)) {
			for (IDecodedPacket packet : demuxer) {
				rest1.add(packet.video().image());
			}
		}

		Assertions.assertEquals(FRAMES, rest1.size());

		try (IDemuxer demuxer = lib.demuxer(file)) {
			readAndVerify(rest1, 0, demuxer, 0);
			readAndVerify(rest1, 0, demuxer, 1);
			seekAndVerify(rest1, 0, demuxer, 6);
			seekAndVerify(rest1, 0, demuxer, 9);
			seekAndVerify(rest1, 0, demuxer, 2);
			seekAndVerify(rest1, 0, demuxer, 5);
			seekAndVerify(rest1, 0, demuxer, 0);
		}
	}

    @ParameterizedTest
    @CsvSource({

    	  "ffv1,         avi",
          "h263p,        avi",
          "mjpeg,        avi",

          "msmpeg4,      avi",
          "msmpeg4v2,    avi",

          "libx264,      mp4",
          "libx264,      mov",

          "libopenh264,  mov",
          "libopenh264,  mp4",

          "libx265,      mp4",
          "libx265,      matroska",
          "wmv1,         avi",
          "wmv2,         avi",
          "mjpeg,        avi",
          "mpeg4,        mp4",

          "libvpx,        webm",
          "libvpx-vp9,    webm",
          "libvpx,        ogg",
          "libvpx,        matroska",
          "libvpx-vp9,    matroska",
    })
	public void testSeekInTwoStreamVideo(String codec, String format) throws IOException {
		File file = dir.resolve("seek2-" + codec + "." + format).toFile();
		System.err.println(file);

		createTwoStreamVideo(file, FRAMES, codec, format);
		List<BufferedImage> restcolor = new ArrayList<>(FRAMES);
		List<BufferedImage> restbw = new ArrayList<>(FRAMES);

		try (IDemuxer demuxer = lib.demuxer(file)) {
			for (IDecodedPacket packet : demuxer) {
				int streamIndex = packet.video().stream().index();
				if (streamIndex == 0) {
					restcolor.add(packet.video().image());
				} else  {
					restbw.add(packet.video().image());
				}
			}
		}

		Assertions.assertEquals(FRAMES, restcolor.size());
		Assertions.assertEquals(FRAMES, restbw.size());

		try (IDemuxer demuxer = lib.demuxer(file)) {
			readAndVerify(restcolor, 0, demuxer, 0);
			seekAndVerify(restbw, 1, demuxer, 9);
			seekAndVerify(restcolor, 0, demuxer, 6);
			seekAndVerify(restcolor, 0, demuxer, 9);
			seekAndVerify(restbw, 1, demuxer, 1);
			seekAndVerify(restcolor, 0, demuxer, 2);
			seekAndVerify(restbw, 1, demuxer, 1);
			seekAndVerify(restbw, 1, demuxer, 4);
			seekAndVerify(restcolor, 0, demuxer, 5);
			seekAndVerify(restcolor, 0, demuxer, 0);
			seekAndVerify(restbw, 1, demuxer, 7);
			seekAndVerify(restbw, 1, demuxer, 0);
			seekAndVerify(restbw, 1, demuxer, 3);
		}
	}

	private void seekAndVerify(List<BufferedImage> rest1, int stream, IDemuxer demuxer, int frameNo) {
		IDecoderVideoStream videoStream = demuxer.videoStream(stream);
		videoStream.seek(frameNo);
		readAndVerify(rest1, stream, demuxer, frameNo);
	}

	private void readAndVerify(List<BufferedImage> rest1, int stream, IDemuxer demuxer, int frameNo) {
		IDecoderVideoStream videoStream = demuxer.videoStream(stream);
		IVideoFrame frame = videoStream.nextFrame();
		Assertions.assertNotNull(frame);
		BufferedImage restored = frame.image();
		assertEqual(restored, rest1.get(frameNo));
	}

}
