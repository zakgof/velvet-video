package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class AVDictionaryEntry extends Struct {
    public AVDictionaryEntry(Runtime runtime) {
        super(runtime);
    }

    public String key = new UTF8StringRef();
    public String value = new UTF8StringRef();
}