package com.zakgof.velvetvideo.impl.middle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zakgof.velvetvideo.impl.JNRHelper;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;
import com.zakgof.velvetvideo.impl.jnr.AVCodecContext;
import com.zakgof.velvetvideo.impl.jnr.AVFrame;
import com.zakgof.velvetvideo.impl.jnr.LibAVFilter;
import com.zakgof.velvetvideo.impl.jnr.LibAVFilter.AVFilter;
import com.zakgof.velvetvideo.impl.jnr.LibAVFilter.AVFilterContext;
import com.zakgof.velvetvideo.impl.jnr.LibAVFilter.AVFilterGraph;
import com.zakgof.velvetvideo.impl.jnr.LibAVFilter.AVFilterInOut;
import com.zakgof.velvetvideo.impl.jnr.LibAVUtil;

import jnr.ffi.Pointer;
import jnr.ffi.Struct;
import jnr.ffi.byref.PointerByReference;
import jnr.ffi.provider.jffi.NativeRuntime;

public class Filters implements AutoCloseable {

		private final static LibAVFilter libavfilter = JNRHelper.load(LibAVFilter.class, "avfilter", 7);
		private static final LibAVUtil libavutil = JNRHelper.load(LibAVUtil.class, "avutil", 56);

		private final Logger logFilter = LoggerFactory.getLogger("velvet-video.filter");

		private final AVFilterContext buffersrc_ctx;
		private final AVFilterContext buffersink_ctx;
		private final Pointer pixfmts;
		private final AVFilterInOut outputs;
		private final AVFilterInOut inputs;
		private final AVFilter buffersrc;
		private final AVFilter buffersink;
		private final AVFilterGraph graph;
		private AVFrame workframe;

		public Filters(AVCodecContext codecCtx, String filterString) {

			graph = libavfilter.avfilter_graph_alloc();

			buffersrc = libavfilter.avfilter_get_by_name("buffer");
			buffersink = libavfilter.avfilter_get_by_name("buffersink");
			outputs = libavfilter.avfilter_inout_alloc();
			inputs = libavfilter.avfilter_inout_alloc();

			PointerByReference ppbuffersink_ctx = new PointerByReference();
			PointerByReference ppbuffersrc_ctx = new PointerByReference();

			String inArgs = String.format("width=%d:height=%d:pix_fmt=%d:time_base=%d/%d", codecCtx.width.get(), codecCtx.height.get(),
					codecCtx.pix_fmt.intValue(), codecCtx.time_base.num.get(), codecCtx.time_base.den.get());

			// TODO?
			pixfmts = NativeRuntime.getInstance().getMemoryManager().allocateDirect(4);
			pixfmts.putInt(0, -1);

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

		public AVFrame submitFrame(AVFrame inputframe) {

			if (workframe == null) {
				workframe = libavutil.av_frame_alloc();
				workframe.width.set(inputframe.width.get());
				workframe.height.set(inputframe.height.get());
				workframe.format.set(inputframe.format.get());
				workframe.pts.set(inputframe.pts.get());
		        libavutil.checkcode(libavutil.av_frame_get_buffer(workframe, 0));
			}

			logFilter.atDebug().log(inputframe == null ? "filter flush" : "frame send to filter PTS=" + inputframe.pts.get());
			libavutil.checkcode(libavfilter.av_buffersrc_write_frame(buffersrc_ctx, inputframe));
			int res = libavfilter.av_buffersink_get_frame(buffersink_ctx, workframe);
			if (res == VelvetVideoLib.AVERROR_EAGAIN || res == VelvetVideoLib.AVERROR_EOF) {
				if (inputframe == null)
					logFilter.atDebug().log("filter buffers empty");
				return null;
			}
			libavutil.checkcode(res);
			logFilter.atDebug().addArgument(workframe.pts.get()).log("filter returned frame PTS={}");

			return workframe;
		}

		public void reset() {
			logFilter.atDebug().log("draining filters");
			while (libavfilter.av_buffersink_get_frame(buffersink_ctx, workframe) >= 0);
		}

		@Override
		public void close() {
			libavfilter.avfilter_inout_free(new Pointer[] {Struct.getMemory(inputs)});
			libavfilter.avfilter_inout_free(new Pointer[] {Struct.getMemory(outputs)});
			libavfilter.avfilter_graph_free(new Pointer[] {Struct.getMemory(graph)});
			libavutil.av_frame_free(new AVFrame[]{workframe});
		}
	}