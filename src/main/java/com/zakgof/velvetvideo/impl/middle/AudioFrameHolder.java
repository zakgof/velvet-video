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

public class AudioFrameHolder implements AutoCloseable, IFrameHolder {

    public final AVFrame frame;
	private final AVRational timebase;
	private final Pointer swrContext;
	private final AVCodecContext codecCtx;
	private AudioFormat targetFormat;
	private AVSampleFormat sampleFormat;

	private static final LibAVUtil libavutil = JNRHelper.load(LibAVUtil.class, "avutil-56");
	private static final LibSwResample libavresample = JNRHelper.load(LibSwResample.class, "swresample-3");

    public AudioFrameHolder(AVRational timebase, boolean encode, AVCodecContext codecCtx, AudioFormat targetFormat) {
        this.frame =  libavutil.av_frame_alloc();
        this.timebase = timebase;
        this.codecCtx = codecCtx;
        this.targetFormat = targetFormat;
        this.sampleFormat = AVSampleFormat.from(targetFormat);
        this.swrContext = initResampler();
    }

    private Pointer initResampler() {
    	Pointer swr = libavresample.swr_alloc();
    	libavutil.av_opt_set_int(swr, "in_channel_count", codecCtx.channels.get(), 0);
    	libavutil.av_opt_set_int(swr, "out_channel_count", targetFormat.getChannels(), 0);
    	libavutil.av_opt_set_int(swr, "in_channel_layout",  (int)codecCtx.channel_layout.get(), 0);
    	libavutil.av_opt_set_int(swr, "out_channel_layout", channelLayout((int)codecCtx.channel_layout.get(), codecCtx.channels.get(), targetFormat.getChannels()),  0);
    	libavutil.av_opt_set_int(swr, "in_sample_rate", codecCtx.sample_rate.get(), 0);
    	libavutil.av_opt_set_int(swr, "out_sample_rate", (int)targetFormat.getSampleRate(), 0);
    	libavutil.av_opt_set_sample_fmt(swr, "in_sample_fmt",  codecCtx.sample_fmt.get(), 0);
    	libavutil.av_opt_set_sample_fmt(swr, "out_sample_fmt", sampleFormat, 0);
        libavutil.checkcode(libavresample.swr_init(swr));
        return swr;
	}

	private int channelLayout(int suggestedLayout, int suggestedChannels, int targetChannels) {
		if (suggestedChannels == targetChannels) {
			return suggestedLayout;
		}
		return libavutil.av_get_default_channel_layout(targetChannels);
	}

	private byte[] samples() {
		Pointer[] buffer = new Pointer[] {null};
		libavutil.av_samples_alloc(buffer, null, targetFormat.getChannels(), frame.nb_samples.get(), sampleFormat, 0);
        int frame_count = libavresample.swr_convert(swrContext, buffer, frame.nb_samples.get(), JNRHelper.ptr(frame.data[0]), frame.nb_samples.get());

    	int bytesCount = frame_count * sampleFormat.bytesPerSample() * targetFormat.getChannels();
    	byte[] b1 = new byte[bytesCount];
    	buffer[0].get(0, b1, 0, bytesCount);
    	return b1;
    }

    @Override
	public void close() {
       libavutil.av_frame_free(new AVFrame[] {frame});
       libavresample.svr_free(new Pointer[] {swrContext});
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
	public IDecodedPacket decode(AbstractDecoderStream stream) {
		return new DecodedAudioPacket(frameOf(stream));
	}

	private IAudioFrame frameOf( DemuxerImpl.AbstractDecoderStream stream) {
		long pts = pts();
		if (pts == LibAVUtil.AVNOPTS_VALUE) {
			pts = 0;
		}

		long nanostamp = pts * 1000000000L * timebase.num.get() / timebase.den.get();
		long duration = libavutil.av_frame_get_pkt_duration(frame);
		long nanoduration = duration * 1000000000L * timebase.num.get() / timebase.den.get();
		return new AudioFrameImpl(samples(), nanostamp, nanoduration, (IDecoderAudioStream)stream);
	}



}