package com.zakgof.velvetvideo.impl.middle;

import javax.sound.sampled.AudioFormat;

import com.zakgof.velvetvideo.IAudioFrame;
import com.zakgof.velvetvideo.IDecoderAudioStream;
import com.zakgof.velvetvideo.impl.JNRHelper;
import com.zakgof.velvetvideo.impl.VelvetVideoLib.DemuxerImpl.AbstractDecoderStream;
import com.zakgof.velvetvideo.impl.jnr.AVCodecContext;
import com.zakgof.velvetvideo.impl.jnr.AVFrame;
import com.zakgof.velvetvideo.impl.jnr.AVRational;
import com.zakgof.velvetvideo.impl.jnr.AVSampleFormat;
import com.zakgof.velvetvideo.impl.jnr.LibAVUtil;
import com.zakgof.velvetvideo.impl.jnr.LibSwResample;

import jnr.ffi.Pointer;
import jnr.ffi.Struct;

public class AudioFrameHolder implements AutoCloseable, IFrameHolder {

    public final AVFrame frame;
	private final AVRational timebase;
	private final Pointer swrContext;
	private final AVCodecContext codecCtx;
	private AudioFormat userFormat;
	private AVSampleFormat userSampleFormat;

	private static final LibAVUtil libavutil = JNRHelper.load(LibAVUtil.class, "avutil", 56);
	private static final LibSwResample libavresample = JNRHelper.load(LibSwResample.class, "swresample", 3);
	private Pointer[] userBuffer;
	private int userBufferSamplesSize;
	private int frameSamples;
	private int bytesPerSample;

    public AudioFrameHolder(AVRational timebase, boolean encode, AVCodecContext codecCtx, AudioFormat userFormat) {
        this.frame =  libavutil.av_frame_alloc();
        this.timebase = timebase;
        this.codecCtx = codecCtx;
        this.userFormat = userFormat;
        this.userSampleFormat = AVSampleFormat.from(userFormat);
        this.swrContext = initResampler(encode);
        this.userBuffer = new Pointer[] {null};
        this.bytesPerSample = userSampleFormat.bytesPerSample() * userFormat.getChannels();
		if (encode) {
			this.frameSamples = codecCtx.frame_size.get();
			frame.nb_samples.set(frameSamples);
			frame.format.set(codecCtx.sample_fmt.longValue());
			frame.channel_layout.set(codecCtx.channel_layout.get());
			frame.sample_rate.set((int)userFormat.getSampleRate());
			libavutil.checkcode(libavutil.av_frame_get_buffer(frame, 0));
		}
	}

    private Pointer initResampler(boolean encode) {
    	Pointer swr = libavresample.swr_alloc();
    	libavutil.av_opt_set_int(swr, !encode ? "in_channel_count" : "out_channel_count", codecCtx.channels.get(), 0);
    	libavutil.av_opt_set_int(swr, !encode ? "out_channel_count" : "in_channel_count", userFormat.getChannels(), 0);
    	libavutil.av_opt_set_int(swr, !encode ? "in_channel_layout" : "out_channel_layout",  (int)codecCtx.channel_layout.get(), 0);
    	libavutil.av_opt_set_int(swr, !encode ? "out_channel_layout" : "in_channel_layout", channelLayout((int)codecCtx.channel_layout.get(), codecCtx.channels.get(), userFormat.getChannels()),  0);
    	libavutil.av_opt_set_int(swr, !encode ? "in_sample_rate" : "out_sample_rate", codecCtx.sample_rate.get(), 0);
    	libavutil.av_opt_set_int(swr, !encode ? "out_sample_rate" : "in_sample_rate", (int)userFormat.getSampleRate(), 0);
    	libavutil.av_opt_set_sample_fmt(swr, !encode ? "in_sample_fmt" : "out_sample_fmt",  codecCtx.sample_fmt.get(), 0);
    	libavutil.av_opt_set_sample_fmt(swr, !encode ? "out_sample_fmt" : "in_sample_fmt", userSampleFormat, 0);
        libavutil.checkcode(libavresample.swr_init(swr));
        return swr;
	}

	private int channelLayout(int suggestedLayout, int suggestedChannels, int targetChannels) {
		if (suggestedChannels == targetChannels) {
			return suggestedLayout;
		}
		return libavutil.av_get_default_channel_layout(targetChannels);
	}

	private byte[] samples(AVFrame frame) {
		reallocUserBuffer(frame.nb_samples.get());
		int frame_count = libavresample.swr_convert(swrContext, userBuffer, userBufferSamplesSize, JNRHelper.ptr(frame.data[0]), userBufferSamplesSize);
    	int bytesCount = frame_count * bytesPerSample;
    	byte[] b1 = new byte[bytesCount];
    	userBuffer[0].get(0, b1, 0, bytesCount);
    	return b1;
    }

	private void reallocUserBuffer(int size) {
		if (size != userBufferSamplesSize) {
			if (this.userBuffer[0] != null) {
				 libavutil.av_freep(userBuffer);
			}
			userBufferSamplesSize = size;
			libavutil.av_samples_alloc(userBuffer, null, userFormat.getChannels(), size, userSampleFormat, 0);
		}
	}

	public int frameBytes() {
		return frameSamples * userSampleFormat.bytesPerSample() * userFormat.getChannels();
	}

	public int put(byte[] samples, int offset) {
		reallocUserBuffer(frameSamples);
		int sampleBytes = Math.min(frameBytes(), samples.length - offset);
		int sampleCount = sampleBytes / bytesPerSample;
		userBuffer[0].put(0, samples, offset, sampleBytes);
		libavutil.checkcode(libavresample.swr_convert(swrContext, JNRHelper.ptr(frame.data[0]), sampleCount, userBuffer, sampleCount));
		frame.nb_samples.set(sampleCount);
		return sampleCount;
	}

    @Override
	public void close() {
       libavutil.av_frame_free(new Pointer[] {Struct.getMemory(frame)});
       libavresample.swr_free(new Pointer[] {swrContext});
       if (this.userBuffer != null) {
    	   libavutil.av_freep(userBuffer);
       }
    }

	@Override
	public AVFrame frame() {
		// TODO
		return frame;
	}

	@Override
	public IAudioFrame decode(AVFrame frame, AbstractDecoderStream stream) {
		long pts = pts();
		if (pts == LibAVUtil.AVNOPTS_VALUE) {
			pts = 0;
		}
		long duration = libavutil.av_frame_get_pkt_duration(frame);
		byte[] samples = samples(frame);
		long nanostamp = pts * 1000000000L * timebase.num.get() / timebase.den.get();
		long nanoduration = duration * 1000000000L * timebase.num.get() / timebase.den.get();
		return new AudioFrameImpl(samples, nanostamp, nanoduration, (IDecoderAudioStream)stream);
	}
}