package com.zakgof.velvetvideo;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.junit.jupiter.api.Assertions;

public class AudioUtil {

	public static File saveWav(AudioFormat format, byte[] buf, File f) {
		AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(buf), format,
				buf.length / format.getFrameSize());
		try {
			AudioSystem.write(ais, AudioFileFormat.Type.WAVE, f);
			return f;
		} catch (Exception e) {
			Assertions.fail(e);
			return null;
		}

	}

}
