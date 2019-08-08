# velvet-video
Java library for encoding/decoding/muxing/demuxing video

velvet-video is using FFmpeg libraries via JNR.

Supported platforms:    
 - Windows 64 bit
 - Linux 64 bit

## Setup

To use `velvet-video`, add the core dependency plus an appropriate native FFmpeg components package.

The choice is:

- velvet-video-natives:free
-- only royalty-free components are included
-- encoders/decoders: Google VP8 and VP9, AOM av1
-- muxers/demuxers: webm, mkv, ogg

- velvet-video-natives:full
-- maximum FFmpeg functionality included
-- the included components use patented technologies and may require royalty fees for commercial usage

````groovy
dependencies {
    compile 'com.github.zakgof:velvet-video-core:0.0.1'
    compile 'com.github.zakgof:velvet-video-natives:free:0.0.1'
}
````

## Quick start

### Encode images into a video:

````java

     IVideoLib lib = new FFMpegVideoLib();
        
      try (IMuxer muxer = lib.muxer("matroska")
         .video("video1", lib.encoder("av1").bitrate(100000))
         .build(new File("/some/path/output.mkv"))) {            

             muxer.video("video1").encode(image1, 0);
             muxer.video("video1").encode(image2, 1);
             muxer.video("video1").encode(image3, 2);
      }
      

````

## License

`velvet-video-core` is dual-licensed under Apache 2.0 and GPL 3.0 (or any later version).

To comply with the FFMpeg components license present bundles into `velvet-video-natives`, choose Apache-2.0 when using with `velvet-video-natives:free` or `GPL-3.0-or-later` when using with `velvet-video-natives:full`

`SPDX-License-Identifier: Apache-2.0 OR GPL-3.0-or-later`

`velvet-video-natives` binaries on jcenter are licensed:

- `velvet-video-natives:free` - under LGPL 3.0 or later
- `velvet-video-natives:full` - under GPL 3.0 or later

`velvet-video-natives` build scripts are licensed under Apache 2.0
