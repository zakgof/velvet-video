package com.zakgof.velvetvideo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class FreeEncodeDecodeTest extends GenericEncodeDecodeTest {

	@Test
    public void testVideoEncodersList() throws IOException {
        List<String> expectedCodecs = Arrays.asList("libvpx", "libvpx-vp9", "libaom-av1");
        codeclist(expectedCodecs, MediaType.Video);
    }

	@Test
    public void testMuxersList() throws IOException {
        List<String> expectedFormats = Arrays.asList("webm", "ogg", "matroska");
        formatlist(expectedFormats, Direction.Encode);
    }

    @ParameterizedTest
    @CsvSource({
         // "libaom-av1,    webm",     // Tooo slooow
         // "libaom-av1,    matroska", // Tooo slooow
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
