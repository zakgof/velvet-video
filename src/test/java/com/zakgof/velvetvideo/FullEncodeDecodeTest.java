package com.zakgof.velvetvideo;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class FullEncodeDecodeTest extends GenericEncodeDecodeTest {

    @Test
    public void testVideoEncodersList() {
        codeclist(Arrays.asList("wmv1", "wmv2", "mjpeg", "mpeg4", "libx264", "libx265", "libvpx", "libvpx-vp9", "libaom-av1"), MediaType.Video);
    }

    @Test
    public void testAudioEncodersList() {
        codeclist(Arrays.asList("aac", "ac3", "libvorbis", "opus", "flac", "mp2", "vorbis"), MediaType.Audio);
    }

    @Test
    public void testMuxersList() {
    	formatlist(Arrays.asList("a64", "ac3", "adts", "adx", "aiff", "amr", "apng", "aptx", "aptx_hd", "asf", "ass", "ast", "asf_stream", "au", "avi", "avm2", "avs2", "bit", "caf", "cavsvideo", "codec2", "codec2raw", "crc", "dash", "data", "daud", "dirac", "dnxhd", "dts", "dv", "eac3", "f4v", "ffmetadata", "fifo", "fifo_test", "filmstrip", "fits", "flac", "flv", "framecrc", "framehash", "framemd5", "g722", "g723_1", "g726", "g726le", "gif", "gsm", "gxf", "h261", "h263", "h264", "hash", "hds", "hevc", "hls", "ico", "ilbc", "image2", "image2pipe", "ipod", "ircam", "ismv", "ivf", "jacosub", "latm", "lrc", "m4v", "md5", "matroska", "matroska", "microdvd", "mjpeg", "mlp", "mmf", "mov", "mp2", "mp3", "mp4", "mpeg", "vcd", "mpeg1video", "dvd", "svcd", "mpeg2video", "vob", "mpegts", "mpjpeg", "mxf", "mxf_d10", "mxf_opatom", "null", "nut", "oga", "ogg", "ogv", "oma", "opus", "alaw", "mulaw", "vidc", "f64be", "f64le", "f32be", "f32le", "s32be", "s32le", "s24be", "s24le", "s16be", "s16le", "s8", "u32be", "u32le", "u24be", "u24le", "u16be", "u16le", "u8", "psp", "rawvideo", "rm", "roq", "rso", "rtp", "rtp_mpegts", "rtsp", "sap", "sbc", "scc", "film_cpk", "segment", "stream_segment,ssegment", "singlejpeg", "smjpeg", "smoothstreaming", "sox", "spx", "spdif", "srt", "sup", "swf", "tee", "3g2", "3gp", "mkvtimestamp_v2", "truehd", "tta", "uncodedframecrc", "vc1", "vc1test", "voc", "w64", "wav", "webm", "webm_dash_manifest", "webm_chunk", "webp", "webvtt", "wtv", "wv", "yuv4mpegpipe"), Direction.Decode);
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
