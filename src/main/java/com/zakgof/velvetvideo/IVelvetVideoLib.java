package com.zakgof.velvetvideo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import javax.sound.sampled.AudioFormat;

import com.zakgof.velvetvideo.impl.FileSeekableInput;

/**
 * Top-level interface for working with velvet-video.
 */
public interface IVelvetVideoLib {

	/**
	 * List available codec names capable of encoding or decoding specific media
	 * type.
	 *
	 * @param dir       direction (encoding or decoding)
	 * @param mediaType media type - audio, video, etc
	 * @return list of codec names
	 */
	List<String> codecs(Direction dir, MediaType mediaType);

	/**
	 * List supported muxer/demuxer media container format types.
	 *
	 * @param dir direction (Encode for muxing, Decode for demuxing)
	 * @return list of format names
	 */
	List<String> formats(Direction dir);

	/**
	 * Create a video encoder builder.
	 *
	 * @param codec requested codec (encoder) name
	 * @return video encoder builder instance
	 */
	IVideoEncoderBuilder videoEncoder(String codec);

	/**
	 * Create an audio encoder builder.
	 *
	 * @param codec requested codec (encoder) name
	 * @param input audio format. If the requested format is not supported by the
	 *              codec, velvet-video will choose the closest compatible audio
	 *              format and will perform format conversion on the fly
	 * @return audio encoder builder instance
	 */
	IAudioEncoderBuilder audioEncoder(String codec, AudioFormat audioFormat);

	/**
	 * Create a remuxer builder from a decoder stream. Remuxer stream can later be
	 * added to a muxer, so the encoded bitstream from the source stream will be
	 * added to the muxer without transcoding.
	 *
	 * @param decoder decoder stream
	 * @return remuxer builder instance
	 */
	IRemuxerBuilder remuxer(IDecoderStream<?, ?, ?> decoder);

	/**
	 * Create a muxer builder for the requested media container format. After
	 * configuring the muxer builder, instantiate the muxer by calling
	 * {@link IMuxerBuilder#build(File)} or
	 * {@link IMuxerBuilder#build(ISeekableOutput)}
	 *
	 * @param format muxer media container format
	 * @return muxer builder instance
	 */
	IMuxerBuilder muxer(String format);

	 /**
     * Open a demuxer to demux from the specified seekable input stream. The demuxer should
     * be closed by calling {@link IDemuxer#close()} after using.
     *
     * @param input input
     * @return demuxer instance
     */
    IDemuxer demuxer(ISeekableInput input);

	/**
	 * Open a demuxer to demux from the specified file
	 *
	 * @param file file to demux
	 * @return demuxer instance
	 */
	default IDemuxer demuxer(File file) {
		try {
			return demuxer(new FileSeekableInput(new FileInputStream(file)));
		} catch (FileNotFoundException e) {
			throw new VelvetVideoException(e);
		}
	}

}
