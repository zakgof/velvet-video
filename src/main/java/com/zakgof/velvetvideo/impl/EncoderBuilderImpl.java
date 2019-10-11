package com.zakgof.velvetvideo.impl;

import java.util.HashMap;
import java.util.Map;

import com.zakgof.velvetvideo.IDecoderVideoStream;
import com.zakgof.velvetvideo.IEncoderBuilder;
import com.zakgof.velvetvideo.IVelvetVideoLib;

class EncoderBuilderImpl implements IEncoderBuilder {

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

        public EncoderBuilderImpl(String codec) {
            this.codec = codec;
        }

        EncoderBuilderImpl(IDecoderVideoStream decoder) {
			this.decoder = decoder;
		}

		@Override
        public IEncoderBuilder framerate(int framerate) {
            this.timebaseNum = 1;
            this.timebaseDen = framerate;
            return this;
        }

        @Override
        public IEncoderBuilder bitrate(int bitrate) {
            this.bitrate = bitrate;
            return this;
        }

        @Override
        public IEncoderBuilder dimensions(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        @Override
        public IEncoderBuilder param(String key, String value) {
            params.put(key, value);
            return this;
        }

        @Override
        public IEncoderBuilder metadata(String key, String value) {
            metadata.put(key, value);
            return this;
        }

        @Override
        public IEncoderBuilder enableExperimental() {
            this.enableExperimental = true;
            return this;
        }
        
        @Override
        public IEncoderBuilder filter(String filter) {
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