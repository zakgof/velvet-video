package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class AVCodec extends Struct {

    public AVCodec(Runtime runtime) {
        super(runtime);
    }

    /**
     * Name of the codec implementation. The name is globally unique among encoders and among decoders (but an encoder and a decoder can share the same name). This is the primary way to find a codec from the user perspective.
     */

    public Struct.String name = new UTF8StringRef();
    /**
     * Descriptive name for the codec, meant to be more human readable than name.
     */
    public Struct.String long_name = new UTF8StringRef();

    public Signed32 type = new Signed32();
    public Signed32 id = new Signed32();
    Signed32 capabilities = new Signed32();
    StructRef<AVRational> supported_framerates = new StructRef<>(AVRational.class); ///< array of supported framerates, or NULL if any, array is terminated by {0,0}
    public Pointer pix_fmts = new Pointer();
}