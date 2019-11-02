package com.zakgof.velvetvideo;

import java.util.ArrayList;
import java.util.List;

public class MemSeekableFile implements ISeekableOutput {

	private static final int CHUNK = 1024*1024;
	private long length;
	private long position;
	private final List<byte[]> buffers = new ArrayList<>();

	@Override
	public void write(byte[] bytes) {
		long bytesToAllocate = position + bytes.length - buffers.size() * CHUNK;
		if (bytesToAllocate > 0) {
			long buffersToAllocate = ((bytesToAllocate - 1)/ CHUNK) * CHUNK + 1;
			for (int i=0; i<buffersToAllocate; i++)
				buffers.add(new byte[CHUNK]);
		}
		for (int messagepos = 0; messagepos < bytes.length; ) {
			long chunk = position / CHUNK;
			long offset = position % CHUNK;
			long len = Math.min(bytes.length - messagepos, CHUNK - offset);
			System.arraycopy(bytes, messagepos, buffers.get((int)chunk), (int)offset, (int)len);
			position += len;
			messagepos += len;
		}
		if (position > length)
			length = position;
	}

	@Override
	public void seek(long position) {
		if (position > length)
			throw new IllegalArgumentException();
		this.position = position;
	}

	@Override
	public void close() {
	}

	public byte[] toBytes() {
		byte[] result = new byte[(int) length];
		for (int pos = 0; pos < length; ) {
			long chunk = pos / CHUNK;
			long offset = pos % CHUNK;
			long len = Math.min(length - pos, CHUNK - offset);
			System.arraycopy(buffers.get((int)chunk), (int)offset, result, pos, (int)len);
			pos += len;
		}
		return result;
	}


}
