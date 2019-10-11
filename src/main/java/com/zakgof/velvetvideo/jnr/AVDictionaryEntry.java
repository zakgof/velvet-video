package com.zakgof.velvetvideo.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.Struct.String;
import jnr.ffi.Struct.UTF8StringRef;

public class AVDictionaryEntry extends Struct {
    public AVDictionaryEntry(Runtime runtime) {
        super(runtime);
    }

    public String key = new UTF8StringRef();
    public String value = new UTF8StringRef();
}