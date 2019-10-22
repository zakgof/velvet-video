package com.zakgof.velvetvideo;

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.zakgof.velvetvideo.impl.VelvetVideoLib;

public class AudioTest extends VelvetVideoTest {

	@Test
	@Disabled
	public void testExisting() throws Exception {
		File file = new File("D:\\Download\\City.on.a.Hill.S01E01.1080p.rus.LostFilm.TV.mkv");
		IVelvetVideoLib lib = new VelvetVideoLib();
		IDecoderAudioStream audioStream = lib.demuxer(file).audioStreams().get(0);
		IAudioFrame frame;
		while ((frame = audioStream.nextFrame())!=null) {
			System.out.println("CLIENT GOT FRAME " + frame.nanoduration()/1000000L + " ms " + frame.samples().length + " bytes");
		}
	}

    @ParameterizedTest
    @CsvSource({
    	 "ac3,             ac3",
    	 "matroska,        ac3",
    	 "mp4,             ac3",
         "ogg,            libvorbis",
         "matroska,       libvorbis",
         "mp4,            libvorbis",
         "webm,           libvorbis",
  //     "matroska,       aac",     8% mismatch
         "mp4,            aac",
         "flac,           flac",
  //     "mp4,            flac",   experimental support
         "matroska,       flac",
    })
	public void testAudioRecodeTest(String format, String codec) throws Exception {
		File src = local("http://www.ee.columbia.edu/~dpwe/sounds/musp/msmn1.wav", "msmn1.wav");
    //	File src = local("https://www2.cs.uic.edu/~i101/SoundFiles/CantinaBand3.wav", "cantina.wav");
		File dest = file("recode-"+ codec +"." + format);
		System.err.println(dest);

		byte[] bufferorig = readAudio(src);
		IVelvetVideoLib lib = new VelvetVideoLib();
		AudioFormat audioFormat = new AudioFormat(Encoding.PCM_SIGNED, 22050, 16, 1, 2, 22050, false);
		try (IMuxer muxer = lib.muxer(format).audioEncoder(lib.audioEncoder(codec, audioFormat).enableExperimental()).build(dest)) {
			IEncoderAudioStream encoder = muxer.audioEncoder(0);
			for (int offset = 0; offset < bufferorig.length; offset+=encoder.frameBytes()) {
				encoder.encode(bufferorig, offset);
			}
		}
		byte[] bufferrecoded = readAudio(dest);
		assertAudioEqual(audioFormat, bufferorig, bufferrecoded);

	}




}
