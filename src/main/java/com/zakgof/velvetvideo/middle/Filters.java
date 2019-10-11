package com.zakgof.velvetvideo.middle;

import java.util.function.Consumer;

import com.zakgof.velvetvideo.FFMpegVideoLib;
import com.zakgof.velvetvideo.JNRHelper;
import com.zakgof.velvetvideo.jnr.AVCodecContext;
import com.zakgof.velvetvideo.jnr.AVFrame;
import com.zakgof.velvetvideo.jnr.LibAVFilter;
import com.zakgof.velvetvideo.jnr.LibAVFilter.AVFilter;
import com.zakgof.velvetvideo.jnr.LibAVFilter.AVFilterContext;
import com.zakgof.velvetvideo.jnr.LibAVFilter.AVFilterGraph;
import com.zakgof.velvetvideo.jnr.LibAVFilter.AVFilterInOut;
import com.zakgof.velvetvideo.jnr.LibAVUtil;

import jnr.ffi.Pointer;
import jnr.ffi.Struct;
import jnr.ffi.byref.PointerByReference;
import jnr.ffi.provider.jffi.NativeRuntime;

public class Filters {

		private final static LibAVFilter libavfilter = JNRHelper.load(LibAVFilter.class, "avfilter-7");
		private static final LibAVUtil libavutil = JNRHelper.load(LibAVUtil.class, "avutil-56");
		
		private AVFilterContext buffersrc_ctx;
		private AVFilterContext buffersink_ctx;
		private Pointer pixfmts;
		private AVFilterInOut outputs;
		private AVFilterInOut inputs;
		private AVFilter buffersrc;
		private AVFilter buffersink;
		private AVFilterGraph graph;
		
		public Filters(FFMpegVideoLib ffMpegVideoLib, AVCodecContext codecCtx, String filterString) {

			graph = libavfilter.avfilter_graph_alloc();

			buffersrc = libavfilter.avfilter_get_by_name("buffer");
			buffersink = libavfilter.avfilter_get_by_name("buffersink");
			outputs = libavfilter.avfilter_inout_alloc();
			inputs = libavfilter.avfilter_inout_alloc();

			PointerByReference ppbuffersink_ctx = new PointerByReference();
			PointerByReference ppbuffersrc_ctx = new PointerByReference();
			
			String inArgs = String.format("width=%d:height=%d:pix_fmt=%d:time_base=%d/%d", codecCtx.width.get(), codecCtx.height.get(),
					codecCtx.pix_fmt.intValue(), codecCtx.time_base.num.get(), codecCtx.time_base.den.get());
			
			pixfmts = NativeRuntime.getInstance().getMemoryManager().allocateDirect(4 * 2);
			pixfmts.putInt(0, -1);
			pixfmts.putInt(4, -1);

			libavutil.checkcode(libavfilter.avfilter_graph_create_filter(ppbuffersrc_ctx, buffersrc, "in", inArgs, null, graph));
			libavutil.checkcode(libavfilter.avfilter_graph_create_filter(ppbuffersink_ctx, buffersink, "out", null, pixfmts, graph));

			
			buffersrc_ctx = JNRHelper.struct(AVFilterContext.class, ppbuffersrc_ctx);
			buffersink_ctx = JNRHelper.struct(AVFilterContext.class, ppbuffersink_ctx);
			

		    outputs.name.set(libavutil.av_strdup("in"));
		    outputs.filter_ctx.set(buffersrc_ctx);
		    outputs.pad_idx.set(0);
		    outputs.next.set((Pointer)null);
		    inputs.name.set(libavutil.av_strdup("out"));
		    inputs.filter_ctx.set(buffersink_ctx);
		    inputs.pad_idx.set(0);
		    inputs.next.set((Pointer)null);
//			
			PointerByReference ins = new PointerByReference(Struct.getMemory(inputs));
			PointerByReference outs = new PointerByReference(Struct.getMemory(outputs));
			
			libavutil.checkcode(libavfilter.avfilter_graph_parse_ptr(graph,
					filterString,
					ins, outs, null));

			libavutil.checkcode(libavfilter.avfilter_graph_config(graph, null));
		}

		public void submitFrame(AVFrame inputframe, AVFrame allocframe, Consumer<AVFrame> output) {
			libavutil.checkcode(libavfilter.av_buffersrc_write_frame(buffersrc_ctx, inputframe));
			int res;
			while ((res = libavfilter.av_buffersink_get_frame(buffersink_ctx, allocframe)) >= 0) {
				output.accept(allocframe);
			}
			if (res == FFMpegVideoLib.AVERROR_EAGAIN) {
				return;
			}
			if (res == FFMpegVideoLib.AVERROR_EOF) {
				output.accept(null);
			}
			libavutil.checkcode(res);
		}
	}