package com.zakgof.velvetvideo;

public interface IRawPacket {
	int streamIndex();

	byte[] bytes();

	long pts();

	long dts();

	long duration();
}
