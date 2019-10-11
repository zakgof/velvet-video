package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Pointer;
import jnr.ffi.annotations.Delegate;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.annotations.StdCall;
import jnr.ffi.byref.PointerByReference;

public interface LibAVFormat {
	
    static final int AVFMT_FLAG_CUSTOM_IO =  0x0080;
    static final int AVFMT_GLOBALHEADER = 0x0040;
	
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

}