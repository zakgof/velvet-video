package com.zakgof.velvetvideo.impl;

import java.util.HashMap;
import java.util.Map;

import com.zakgof.velvetvideo.IDecoderVideoStream;

abstract class AbstractEncoderBuilderImpl<I> {

	String codec;
	Integer timebaseNum;
	Integer timebaseDen;
	Integer bitrate;
	Map<String, String> params = new HashMap<>();
	Map<String, String> metadata = new HashMap<>();
	boolean enableExperimental;
	IDecoderVideoStream decoder;
	String filter;

	AbstractEncoderBuilderImpl(String codec) {
		this.codec = codec;
	}

	AbstractEncoderBuilderImpl(IDecoderVideoStream decoder) {
		this.decoder = decoder;
	}

	public I framerate(int framerate) {
		this.timebaseNum = 1;
		this.timebaseDen = framerate;
		return self();
	}

	@SuppressWarnings("unchecked")
	private I self() {
		return (I) this;
	}

	public I framerate(int num, int den) {
		this.timebaseNum = num;
		this.timebaseDen = den;
		return self();
	}

	public I bitrate(int bitrate) {
		this.bitrate = bitrate;
		return self();
	}

	public I param(String key, String value) {
		params.put(key, value);
		return self();
	}

	public I metadata(String key, String value) {
		metadata.put(key, value);
		return self();
	}

	public I enableExperimental() {
		this.enableExperimental = true;
		return self();
	}

	public I filter(String filter) {
		this.filter = filter;
		return self();
	}

}