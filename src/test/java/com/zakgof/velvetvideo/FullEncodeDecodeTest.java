package com.zakgof.velvetvideo;

import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class FullEncodeDecodeTest extends GenericEncodeDecodeTest {
    
    @ParameterizedTest
    @ValueSource(strings = {"libvpx", "libvpx-vp9", "libaom-av1"})
    public void testCodeclist(String codec) throws IOException {
        codeclist(codec);
    }

    @ParameterizedTest
    @CsvSource({
         "libvpx,        webm",
         "libvpx-vp9,    webm",
         "libvpx,        ogg",
         "libvpx,        matroska",
         "libvpx-vp9,    matroska"
    })
    public void testEncodeDecodeCompare(String codec, String format) throws IOException {  
        encodeDecodeCompare(codec, format);
    }

}
