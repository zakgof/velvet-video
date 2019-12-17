# velvet-video
Java library for encoding/decoding/muxing/demuxing video and audio

With velvet-video it's easy to:
- create video from still images
- extract frames from a video
- extract audio tracks from video files
- remux video or audio from one container format to another (like mp4 to mkv)
- transcode (recompress) videos/audios using another codec (like x264 to vp9 or wav to mp3)
- change video timing (slo-mo, timelapse etc)
- merge videos or split them to segments
- apply filters of transformations (before encoding or after decoding)

velvet-video supports dozens of container formats (including mp4, avi, webm, matroska) and codecs (including x264, hevc, vp9, av1).


[![Travis CI](https://travis-ci.org/zakgof/velvet-video.svg?branch=release)](https://travis-ci.org/zakgof/velvet-video)
[![velvet-video on bintray](https://api.bintray.com/packages/zakgof/maven/velvet-video/images/download.svg)](https://bintray.com/zakgof/maven/velvet-video)

velvet-video embeds FFmpeg libraries under the hood, so it works at native speed and uses all FFmpeg's hardware optimization.
Extracting and loading native libs is fully covered by velvet-video.

Supported platforms:    
 - Windows 64 bit
 - Linux 64 bit
 
Please contact me if you need support for a platform not listed here. 

## Setup

To use `velvet-video` add the core dependency plus an appropriate native FFmpeg components package. `velvet-video` is available on [bintray](https://bintray.com/zakgof/maven/velvet-video)

The choice for native package is:

- `velvet-video-natives:free`
   - only royalty-free components are included
   - encoders/decoders: Google VP8 and VP9, AOM av1
   - muxers/demuxers: webm, mkv, ogg

- `velvet-video-natives:full`
   - maximum FFmpeg functionality included
   - the included components use patented technologies and may require royalty fees for commercial usage

### gradle
````groovy
dependencies {
    compile 'com.github.zakgof:velvet-video:0.4.0'
    compile 'com.github.zakgof:velvet-video-natives:0.2.6.full'
}
````
### maven
````xml
<dependency>
  <groupId>com.github.zakgof</groupId>
  <artifactId>velvet-video</artifactId>
  <version>0.4.0</version>
  <type>pom</type>
</dependency>
<dependency>
  <groupId>com.github.zakgof</groupId>
  <artifactId>velvet-video-natives</artifactId>
  <version>0.2.6.full</version>
  <type>pom</type>
</dependency>
````

## Quick start

### Encode images into a video:

````java
    IVelvetVideoLib lib = VelvetVideoLib().getInstance();
    try (IMuxer muxer = lib.muxer("matroska")
        .video(lib.videoEncoder("libaom-av1").bitrate(800000))
        .build(new File("/some/path/output.mkv"))) {
             IEncoderVideoStream videoStream = muxer.videoStream(0);	        
             videoStream.encode(image1);
             videoStream.encode(image2);
             videoStream.encode(image3);
    }      
````
### Obtain images from a video:

````java
	IVelvetVideoLib lib = VelvetVideoLib().getInstance();
	try (IDemuxer demuxer = lib.demuxer(new File("/some/path/example.mp4"))) {
	    IDecoderVideoStream videoStream = demuxer.videoStream(0);
	    IFrame videoFrame;
	    while ((videoFrame = videoStream.nextFrame()) != null) {
	   	    BufferedImage image = videoFrame.image();
	   	    // Use image as needed...
	    }
	}      
````
### More examples

 - Extract audio from mkv file as mp3
 - Play audio file
 
 https://github.com/zakgof/velvet-video/tree/master/src/example/java/com/zakgof/velvetvideo/example

### Advanced example: video player

See https://github.com/zakgof/velvet-video-player

## License

`velvet-video-core` is dual-licensed under Apache 2.0 and GPL 3.0 (or any later version).

To comply with the FFMpeg components license present bundles into `velvet-video-natives`, choose `Apache-2.0` when using with `velvet-video-natives:free` or `GPL-3.0-or-later` when using with `velvet-video-natives:full`

`SPDX-License-Identifier: Apache-2.0 OR GPL-3.0-or-later`

`velvet-video-natives` binaries on jcenter are licensed:

- `velvet-video-natives:free` - under LGPL 3.0 or later
- `velvet-video-natives:full` - under GPL 3.0 or later

`velvet-video-natives` build scripts are licensed under Apache 2.0
