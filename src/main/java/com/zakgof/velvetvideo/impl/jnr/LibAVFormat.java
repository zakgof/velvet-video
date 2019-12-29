package com.zakgof.velvetvideo.impl.jnr;

import java.util.ArrayList;
import java.util.List;

import com.zakgof.velvetvideo.Direction;

import jnr.ffi.Pointer;
import jnr.ffi.annotations.Delegate;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.annotations.StdCall;
import jnr.ffi.byref.PointerByReference;

public interface LibAVFormat {

    static final int AVFMT_FLAG_CUSTOM_IO =  0x0080;
    static final int AVFMT_GLOBALHEADER = 0x0040;

    static final int  AVSEEK_FLAG_BACKWARD = 1; ///< seek backward
    static final int  AVSEEK_FLAG_BYTE     = 2; ///< seeking based on position in bytes
    static final int  AVSEEK_FLAG_ANY      = 4; ///< seek to any frame, even non-keyframes
    static final int  AVSEEK_FLAG_FRAME    = 8;

    AVInputFormat av_demuxer_iterate(PointerByReference opaque);

    AVOutputFormat av_muxer_iterate	(PointerByReference opaque);

	int avformat_alloc_output_context2(@Out PointerByReference ctx, AVOutputFormat oformat, String format_name,
			String filename);

	AVStream avformat_new_stream(AVFormatContext ctx, @In AVCodec codec);

	int avformat_write_header(AVFormatContext ctx, Pointer[] dictionary);

	int av_write_trailer(AVFormatContext ctx);

	void avformat_free_context(AVFormatContext ctx);

	AVOutputFormat av_guess_format(String short_name, String filename, String mime_type);

	int av_write_frame(AVFormatContext context, AVPacket packet);

	int av_interleaved_write_frame(AVFormatContext ctx, AVPacket packet);

	AVIOContext avio_alloc_context(Pointer buffer, int buffer_size, int write_flag, Pointer opaque,
			LibAVFormat.IPacketIO reader, LibAVFormat.IPacketIO writer, LibAVFormat.ISeeker seeker);

	void avio_context_free(Pointer[] avioContext);

	int avformat_open_input(PointerByReference ctx, String url, AVInputFormat fmt, Pointer[] options);

	interface IPacketIO {
		@Delegate
		@StdCall
		int read_packet(Pointer opaque, Pointer buf, int buf_size);
	}

	interface ISeeker {
		@Delegate
		@StdCall
		int seek(Pointer opaque, int offset, int whence);
	}

	interface ICustomAvioCallback extends LibAVFormat.IPacketIO, LibAVFormat.ISeeker {
	}

	void av_dump_format(AVFormatContext context, int i, String string, int j);

	int avio_open(PointerByReference pbref, String url, int flags);

	AVFormatContext avformat_alloc_context();

	int avformat_find_stream_info(AVFormatContext context, Pointer[] options);

	int av_find_best_stream(AVFormatContext context, int type, int wanted_stream_nb, int related_stream,
			Pointer[] decoder_ret, int flags);

	int av_read_frame(AVFormatContext context, AVPacket pkt);

	int av_seek_frame(AVFormatContext context, int stream_index, long timestamp, int flags);

	default List<String> formats(Direction dir) {
		List<String> formats = new ArrayList<>();
		PointerByReference ptr = new PointerByReference();
		if (dir == Direction.Encode) {
			AVOutputFormat format;
			while ((format = av_muxer_iterate(ptr)) != null) {
				formats.add(format.name.get());
			}
		} else if (dir == Direction.Decode) {
			AVInputFormat format;
			while ((format = av_demuxer_iterate(ptr)) != null) {
				formats.add(format.name.get());
			}
		}
		return formats;
	}

}