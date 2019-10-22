package com.zakgof.velvetvideo;

public interface IRemuxerBuilder {

	IRemuxerBuilder framerate(int framerate);

	IRemuxerBuilder framerate(int num, int den);

}
