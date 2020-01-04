package com.zakgof.velvetvideo;

/**
 * Raw (not decoded) media packet.
 */
public interface IRawPacket {

	/**
	 * @return index of the stream in the container
	 */
	int streamIndex();

	/**
	 * @return packet bytes
	 */
	byte[] bytes();

	/**
	 * @return presentation timestamp (in stream time base units)
	 */
	long pts();

	/**
	 * @return decoding timestamp (in stream time base units)
	 */
	long dts();

	/**
	 * @return duration of the packet (in stream time base units)
	 */
	long duration();
}
