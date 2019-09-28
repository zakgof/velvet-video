package com.zakgof.velvetvideo;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class FullEncodeDecodeTest extends GenericEncodeDecodeTest {

    @Test
    public void testCodeclist() throws IOException {
        codeclist(Arrays.asList("wmv1", "wmv2", "mjpeg", "mpeg4", "libx264", "libx265", "libvpx", "libvpx-vp9", "libaom-av1"));
    }

    @ParameterizedTest
    @CsvSource({

         "ffv1,         avi",
         "flv,          flv",
         "h263p,        avi",
         "mjpeg,        avi",

         "msmpeg4,      avi",
         "msmpeg4v2,    avi",
         "msmpeg4v2,    matroska",

         "libx264,      mp4",
         "libx264,      avi",
         "libx264,      mov",
     //  "libx264,      matroska", // FAIL - Invalid data

         "libopenh264,  avi",
         "libopenh264,  mov",
         "libopenh264,  mp4",
     //  "libopenh264,  matroska", // FAIL - Invalid data

         "libx265,      mp4",
         "libx265,      matroska",
         "wmv1,         avi",
     //  "wmv2,         avi", // FAIL: restored frame mismatch (!)
         "mjpeg,        avi",
         "mpeg1video,   avi",
         "mpeg2video,   avi",
         "mpeg4,        mp4",
      // "dvvideo,      avi", // FAIL: Need a matching profile

         "libvpx,        webm",
         "libvpx-vp9,    webm",
         "libvpx,        ogg",
         "libvpx,        matroska",
         "libvpx-vp9,    matroska",
    })
    public void testEncodeDecodeCompare(String codec, String format) throws IOException {
        encodeDecodeCompare(codec, format);
    }

}
