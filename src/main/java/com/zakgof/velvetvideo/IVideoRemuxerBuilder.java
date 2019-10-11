package com.zakgof.velvetvideo;

public interface IVideoRemuxerBuilder {

	IVideoRemuxerBuilder framerate(int framerate);

	IVideoRemuxerBuilder framerate(int num, int den);

}
