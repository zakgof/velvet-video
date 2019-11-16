package com.zakgof.velvetvideo.impl.middle;

import javax.sound.sampled.AudioFormat;

import com.zakgof.velvetvideo.IAudioFrame;
import com.zakgof.velvetvideo.IDecodedPacket;
import com.zakgof.velvetvideo.IDecoderAudioStream;
import com.zakgof.velvetvideo.impl.JNRHelper;
import com.zakgof.velvetvideo.impl.VelvetVideoLib.DemuxerImpl;
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
	private long nextPts;
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
		System.err.println("samples() " + frame.nb_samples.get());
		reallocUserBuffer(frame.nb_samples.get());
		int frame_count = libavresample.swr_convert(swrContext, userBuffer, userBufferSamplesSize, JNRHelper.ptr(frame.data[0]), userBufferSamplesSize);
    	int bytesCount = frame_count * bytesPerSample;
    	byte[] b1 = new byte[bytesCount];
    	userBuffer[0].get(0, b1, 0, bytesCount);
    	return b1;
    }

	private void reallocUserBuffer(int size) {
		if (size != userBufferSamplesSize) {
			if (this.userBuffer != null) {
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
	public long pts() {
		// TODO
		return frame.pts.get();
	}

	@Override
	public AVFrame frame() {
		// TODO
		return frame;
	}

	@Override
	public IDecodedPacket decode(AVFrame frame, AbstractDecoderStream stream) {
		return new DecodedAudioPacket(frameOf(stream, frame));
	}

	private IAudioFrame frameOf(DemuxerImpl.AbstractDecoderStream stream, AVFrame frame) {
		long pts = pts();
		if (pts == LibAVUtil.AVNOPTS_VALUE) {
			pts = 0;
		}
		long duration = libavutil.av_frame_get_pkt_duration(frame);
		byte[] samples = samples(frame);

//		if (pts > nextPts) {
//			System.err.println("BAD PTS: GAP " + nextPts + " --> " + pts); // TODO
//			int offset = (int) (pts - nextPts);
//			int bytepadding = offset * bytesPerSample;
//			byte[] oldsamples = samples;
//			samples = new byte[bytepadding + oldsamples.length];
//			System.arraycopy(oldsamples, 0, samples, bytepadding, oldsamples.length);
//			duration += offset;
//		}
//		if (pts < nextPts) {
//			// TODO: log warning
//			long offset = nextPts - pts;
//			System.err.println("BAD PTS: OVERLAP " + nextPts + " --> " + pts); // TODO
//			if (samples.length < offset * bytesPerSample) {
//				samples = new byte[] {};
//				duration = 0;
//			} else {
//				samples = Arrays.copyOfRange(samples, (int) (offset * bytesPerSample), samples.length);
//				duration = duration - offset;
//			}
//			pts = nextPts;
//
//			System.err.println("FIXING PTS !!!!");
//		}

		long nanostamp = pts * 1000000000L * timebase.num.get() / timebase.den.get();
		nextPts = pts + duration;
		long nanoduration = duration * 1000000000L * timebase.num.get() / timebase.den.get();
		return new AudioFrameImpl(samples, nanostamp, nanoduration, (IDecoderAudioStream)stream);
	}



}