package com.zakgof.velvetvideo;

public interface IEncoderBuilder<I extends IEncoderBuilder<?>> {
	I framerate(int framerate);

	I framerate(int num, int den);

	I param(String key, String value);

	I metadata(String key, String value);

	I enableExperimental();

	I filter(String filter);

	I bitrate(int bitrate);
}
