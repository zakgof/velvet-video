package com.zakgof.velvetvideo.impl;

import java.util.HashMap;
import java.util.Map;

import com.zakgof.velvetvideo.IDecoderVideoStream;
import com.zakgof.velvetvideo.IVideoEncoderBuilder;

class VideoEncoderBuilderImpl implements IVideoEncoderBuilder {

	String codec;
	Integer timebaseNum;
	Integer timebaseDen;
	Integer bitrate;
	Map<String, String> params = new HashMap<>();
	Map<String, String> metadata = new HashMap<>();
	Integer width;
	Integer height;
	boolean enableExperimental;
	IDecoderVideoStream decoder;
	String filter;

	public VideoEncoderBuilderImpl(String codec) {
		this.codec = codec;
	}

	VideoEncoderBuilderImpl(IDecoderVideoStream decoder) {
		this.decoder = decoder;
	}

	@Override
	public IVideoEncoderBuilder framerate(int framerate) {
		this.timebaseNum = 1;
		this.timebaseDen = framerate;
		return this;
	}

	@Override
	public IVideoEncoderBuilder framerate(int num, int den) {
		this.timebaseNum = num;
		this.timebaseDen = den;
		return this;
	}

	@Override
	public IVideoEncoderBuilder bitrate(int bitrate) {
		this.bitrate = bitrate;
		return this;
	}

	@Override
	public IVideoEncoderBuilder dimensions(int width, int height) {
		this.width = width;
		this.height = height;
		return this;
	}

	@Override
	public IVideoEncoderBuilder param(String key, String value) {
		params.put(key, value);
		return this;
	}

	@Override
	public IVideoEncoderBuilder metadata(String key, String value) {
		metadata.put(key, value);
		return this;
	}

	@Override
	public IVideoEncoderBuilder enableExperimental() {
		this.enableExperimental = true;
		return this;
	}

	@Override
	public IVideoEncoderBuilder filter(String filter) {
		this.filter = filter;
		return this;
	}

//        private EncoderImpl build(IPacketStream stream) {
//            return new EncoderImpl(stream, this);
//        }
//
//        @Override
//        public IEncoder build(OutputStream output) {
//            return build(new OutputPacketStream(output));
//        }

}