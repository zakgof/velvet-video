package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;

import com.zakgof.velvetvideo.IVideoLib.Direction;
import com.zakgof.velvetvideo.IVideoLib.IDecodedPacket;
import com.zakgof.velvetvideo.IVideoLib.IDemuxer;
import com.zakgof.velvetvideo.IVideoLib.IFrame;

public class GenericEncodeDecodeTest extends VelvetVideoTest {

    private static final int FRAMES = 16;

    protected void codeclist(Collection<String> expectedCodecs) throws IOException {
        List<String> codecs = lib.codecs(Direction.Encode);
        System.err.println(codecs);
        Set<String> codecSet = new HashSet<>(expectedCodecs);
        codecSet.removeAll(codecs);
        Assertions.assertTrue(codecSet.isEmpty(), "Missing codecs: " + codecSet);
    }

    protected void encodeDecodeCompare(String codec, String format) throws IOException {

        File file = dir.resolve(codec + "." + format).toFile();
        System.err.println(file);

        BufferedImage[] orig = createSingleStreamVideo(codec, format, file, FRAMES);

        double dff = diff(orig[0], orig[1]);
        System.err.println("[0] to [1] " + dff);
        try (IDemuxer demuxer = lib.demuxer(file)) {
            int i = 0;
			for (IDecodedPacket packet : demuxer) {
				Assertions.assertTrue(i < FRAMES);
				IFrame frame = packet.video();
				BufferedImage imgrestored = frame.image();
				try {
					// TODO
					ImageIO.write(orig[i], "png", new File("c:\\pr\\orig-" + i + ".png"));
					ImageIO.write(imgrestored, "png", new File("c:\\pr\\rest-" + i + ".png"));
				} catch (IOException e) {
					Assertions.fail(e);
				}
				double diff = diff(orig[i], imgrestored);
				System.err.println("Diff for frame " + i + " = " + diff);
				Assertions.assertEquals(0, diff, 20.0, "Frame " + i + " differs by " + diff);
				i++;
			}
			Assertions.assertEquals(FRAMES, i);
        }
    }


	public void testEncodeDecodeTwoStreams() throws IOException {

		File file = dir.resolve("two.mp4").toFile();
		BufferedImage[][] origs = createTwoStreamVideo(file, FRAMES);

		int colorindex = 0;
		int bwindex = 0;

		try (IDemuxer demuxer = lib.demuxer(file)) {
			for (IDecodedPacket packet : demuxer) {
				if (packet.video().stream().name().equals("color")) {
					BufferedImage imgrestored = packet.video().image();
					double diff = diff(origs[0][colorindex], imgrestored);
					Assertions.assertEquals(0, diff, 10.0, "Color frame " + colorindex + " differs by " + diff);
					colorindex++;
				}
				if (packet.video().stream().name().equals("bw")) {
					BufferedImage imgrestored = packet.video().image();
					double diff = diff(origs[1][bwindex], imgrestored);
					Assertions.assertEquals(0, diff, 10.0, "BW rame " + bwindex + " differs by " + diff);
					bwindex++;
				}
			}
		}
		Assertions.assertEquals(FRAMES, colorindex);
		Assertions.assertEquals(FRAMES, bwindex);
	}







}
