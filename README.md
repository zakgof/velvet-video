# velvet-video
Java library for encoding/decoding/muxing/demuxing video

````groovy
dependencies {
    compile 'com.github.zakgof:velvet-video:0.0.1'
    compile 'com.github.zakgof:velvet-video-natives:rf:0.0.1'
}
````

````java

     IVideoLib lib = new FFMpegVideoLib();
        
        System.exit(4);
        try (IMuxer muxer = lib.muxer("mp4")
            .video("video1", lib.encoder("libx264").bitrate(100000))
            .build(new File("c:\\pr\\1.mp4"))) {            
                muxer.video("video1").encode(image, 0);
                muxer.video("video1").encode(image, 1);
            }
        }

````

## License

This work is dual-licensed under Apache 2.0 and GPL 3.0 (or any later version).
You can choose between one of them if you use this work.

`SPDX-License-Identifier: Apache-2.0 OR GPL-3.0-or-later`
