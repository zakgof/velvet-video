package com.zakgof.velvetvideo;

public interface IRemuxerStream {
	void writeRaw(byte[] packetData);

	void writeRaw(IRawPacket rawPacket);
}