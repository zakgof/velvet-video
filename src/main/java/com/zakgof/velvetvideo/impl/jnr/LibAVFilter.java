package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.annotations.In;
import jnr.ffi.byref.PointerByReference;

public interface LibAVFilter {

	AVFilter avfilter_next(AVFilter prev);

	AVFilterGraph avfilter_graph_alloc();

	AVFilter avfilter_get_by_name(String name);

	AVFilterContext avfilter_graph_alloc_filter(AVFilterGraph graph, AVFilter filter, String name);

	int avfilter_graph_config(AVFilterGraph graphctx, Pointer log_ctx);

	int avfilter_graph_parse2(AVFilterGraph graph, String filters, PointerByReference /* AVFilterInOut */ inputs,
			PointerByReference/* AVFilterInOut */ outputs);

	int avfilter_graph_parse_ptr(AVFilterGraph graph, String filters, PointerByReference /* AVFilterInOut */ inputs,
			PointerByReference/* AVFilterInOut */ outputs, Pointer log);

	void avfilter_free(AVFilterContext filter);

	void avfilter_graph_free(Pointer ppgraph);

	AVFilterInOut avfilter_inout_alloc();

	int avfilter_graph_create_filter(PointerByReference /* AVFilterContext */ filt_ctx, @In AVFilter filt, String name,
			String args, Pointer opaque, AVFilterGraph graph_ctx);

	int av_buffersrc_add_frame(AVFilterContext ctx, @In AVFrame frame);

	int av_buffersrc_write_frame(AVFilterContext ctx, @In AVFrame frame);

	int av_buffersink_get_frame(AVFilterContext ctx, AVFrame frame);

	int avfilter_link(AVFilterContext incontext, int inpad, AVFilterContext outcontext, int outpad);

	public static class AVFilter extends Struct {
		public AVFilter(Runtime runtime) {
			super(runtime);
		}
	}

	public static class AVFilterGraph extends Struct {
		public AVFilterGraph(Runtime runtime) {
			super(runtime);
		}
	}

	public static class AVFilterContext extends Struct {
		public AVFilterContext(Runtime runtime) {
			super(runtime);
		}

		Pointer av_class = new Pointer();
		StructRef<AVFilter> filter = new StructRef<>(AVFilter.class);
		String name = new AsciiStringRef();
		Pointer input_pads = new Pointer();
		Pointer inputs = new Pointer();
		Unsigned32 input_count = new Unsigned32();
		Unsigned32 nb_inputs = new Unsigned32();
		Pointer output_pads = new Pointer();
		Pointer outputs = new Pointer();
		Unsigned32 output_count = new Unsigned32();
		Unsigned32 nb_outputs = new Unsigned32();
		Pointer priv = new Pointer();
		Pointer command_queue = new Pointer();
	}

	public static class AVFilterInOut extends Struct {
		public AVFilterInOut(Runtime runtime) {
			super(runtime);
		}

		public Pointer name = new Pointer();
		public StructRef<AVFilterContext> filter_ctx = new StructRef<>(AVFilterContext.class);
		public Signed32 pad_idx = new Signed32();
		public Pointer next = new Pointer();
	}

}