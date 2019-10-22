package com.zakgof.velvetvideo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class FreeEncodeDecodeTest extends GenericEncodeDecodeTest {

    public void testVideoEncodersList() throws IOException {
        List<String> expectedCodecs = Arrays.asList("libvpx", "libvpx-vp9", "libaom-av1");
        codeclist(expectedCodecs, MediaType.Video);
    }

    @ParameterizedTest
    @CsvSource({
//       "libaom-av1,    webm",    // ERROR: muxer returns "Invalid data found when processing input"
//       "libaom-av1,    matroska", // ERROR: muxer returns "Invalid data found when processing input"
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
