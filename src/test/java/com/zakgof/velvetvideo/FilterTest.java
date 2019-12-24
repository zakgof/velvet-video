package com.zakgof.velvetvideo;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.zakgof.velvetvideo.impl.VelvetVideoLib;

public class FilterTest extends VelvetVideoTest {


	private static final int FRAME_COUNT = 10;
	private static final String FLIP_FILTER = "split [main][tmp]; [tmp] crop=iw:ih/2:0:0, vflip [flip]; [main][flip] overlay=0:H/2";

	@Test
	public void testFlipBeforeEncoding() {
		File file = dir.resolve("before-filter.webm").toFile();
		System.err.println(file);
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();
		encodeVideoWithFilter(file, lib, FLIP_FILTER);
		checkFlipped(file, lib, null);
	}

	@Test
	public void testFlipAfterDecoding() {
		File file = dir.resolve("after-filter.webm").toFile();
		System.err.println(file);
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();
		encodeVideoWithFilter(file, lib, null);

		checkFlipped(file, lib, FLIP_FILTER);
	}

	 @ParameterizedTest
	    @CsvSource({
//	    	 "atadenoise=s=7:p=7:0a=0:0b=0:1a=0:1b=0:2a=0:2b=0",
//			 "atadenoise",
		 	 "bm3d",
			 "fftdnoiz",
			 "hqdn3d",
			 "nlmeans",
			 "fftdnoiz",
			 "owdenoise",
			 "vaguedenoiser",
			 "fftdnoiz"
	    })
	public void testTemporalFilters(String filter) {
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();
		File pre  = dir.resolve("temporal-pre"  + filter.replace(':', '-') + ".webm").toFile();
		File post = dir.resolve("temporal-post" + filter.replace(':', '-') + ".webm").toFile();
		File nof = dir.resolve("temporal-nof" + filter.replace(':', '-') + ".webm").toFile();

		System.err.println(pre + " / " + post);

		encodeVideoWithFilter(pre, lib, filter);
		List<BufferedImage> preframes = loadFrames(pre, FRAME_COUNT);

		encodeVideoWithFilter(post, lib, null);
		List<BufferedImage> postframes = loadFrames(post, FRAME_COUNT, filter);

		encodeVideoWithFilter(nof, lib, null);
		List<BufferedImage> refframes = loadFrames(nof, FRAME_COUNT, null);

		Assertions.assertEquals(FRAME_COUNT, postframes.size());
		Assertions.assertEquals(FRAME_COUNT, preframes.size());
		for (int i=0; i<FRAME_COUNT; i++) {


			System.err.println(" *********** " + filter + " **************");
			System.err.println(" pre to ref  " + diff(preframes.get(i), refframes.get(i)));
			System.err.println("post to ref  " + diff(postframes.get(i), refframes.get(i)));
			System.err.println("post to pre  " + diff(postframes.get(i), preframes.get(i)));
			System.err.println();

			if (i>7)
				assertEqual(preframes.get(i), postframes.get(i), 10.0);
		}

	}

	private void checkFlipped(File file, IVelvetVideoLib lib, String filterString) {
		try (IDemuxer demuxer = lib.demuxer(file)) {
			IVideoDecoderStream videoStream = demuxer.videoStream(0);
			videoStream.setFilter(filterString);
			for (int i=0; i<FRAME_COUNT; i++) {
				IVideoFrame frame = videoStream.nextFrame();
				BufferedImage image = frame.image();
				Assertions.assertTrue(isFlipped(image));
			}
		}
	}

	private void encodeVideoWithFilter(File file, IVelvetVideoLib lib, String filterString) {
		IVideoEncoderBuilder encoder = lib.videoEncoder("libvpx")
			.filter(filterString)
			.dimensions(640, 480)
			.bitrate(20000000)
			.framerate(3, 1);

		try (IMuxer muxer = lib.muxer("webm")
		    .videoEncoder(encoder)
		    .build(file)) {

			IVideoEncoderStream videoStream = muxer.videoEncoder(0);
			for (int i=0; i<FRAME_COUNT; i++) {
				BufferedImage image = colorImageNoisy(i);
				videoStream.encode(image);
				Assertions.assertFalse(isFlipped(image));
			}
		}
	}

	private boolean isFlipped(BufferedImage image) {
		WritableRaster raster = image.getRaster();
		int[] pixel1 = new int[3];
		int[] pixel2 = new int[3];
		return
		isPixelFlipped( 20,  20, raster, pixel1, pixel2) &&
		isPixelFlipped(242,  17, raster, pixel1, pixel2) &&
		isPixelFlipped(181, 289, raster, pixel1, pixel2) &&
		isPixelFlipped(562, 318, raster, pixel1, pixel2) &&
		isPixelFlipped(429,  88, raster, pixel1, pixel2) &&
		isPixelFlipped(344,  67, raster, pixel1, pixel2);
	}

	private boolean isPixelFlipped(int x, int y, WritableRaster raster, int[] pixel1, int[] pixel2) {
		raster.getPixel(x, y, pixel1);
		raster.getPixel(x, raster.getHeight()-y-1, pixel2);
		int diff = (pixel1[0]-pixel2[0])*(pixel1[0]-pixel2[0]) + (pixel1[1]-pixel2[1])*(pixel1[1]-pixel2[1]) + (pixel1[2]-pixel2[2])*(pixel1[2]-pixel2[2]);
		System.err.println("Diff=" + diff);
		return diff < 300;
	}

}
