package com.zakgof.velvetvideo;

/**
 * Muxer's media stream without any encoder attached. The stream is capable of writing raw (already encoded) packet of any media type.
 */
public interface IRemuxerStream {

	/**
	 * Writes raw packet data. A typical case is <i>remuxing</i> - writing packets read from another container with the method {@link IDemuxer#nextRawPacket()}.
	 * @param packetData raw packet bytes.
	 */
	void writeRaw(byte[] packetData);

	// void writeRaw(IRawPacket rawPacket);
}