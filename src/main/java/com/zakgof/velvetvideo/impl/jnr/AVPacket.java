package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class AVPacket extends Struct {

    public AVPacket(Runtime runtime) {
        super(runtime);
    }

    public Pointer buf = new Pointer();
    public int64_t pts = new int64_t();
    public int64_t dts = new int64_t();

    public Pointer data = new Pointer();
    public Signed32 size = new Signed32();
    public Signed32 stream_index = new Signed32();
    public Signed32 flags = new Signed32();
 	public Pointer side_data = new Pointer();
 	public Signed32 side_data_elems = new Signed32();
 	public int64_t duration = new int64_t();
 	public int64_t pos = new int64_t();
 	public int64_t convergence_duration = new int64_t();

 	public byte[] bytes() {
		byte[] raw = new byte[size.get()];
		data.get().get(0, raw, 0, raw.length);
		return raw;
	}
}