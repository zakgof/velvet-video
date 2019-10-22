package com.zakgof.velvetvideo.impl.jnr;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;

public class AVCodecContext extends Struct {

    public AVCodecContext(Runtime runtime) {
        super(runtime);
    }

    public Struct.Pointer av_class = new Pointer(); // const AVClass *av_class;
    public Signed32 log_level_offset = new Signed32();
    public Signed32 codec_type = new Signed32();
    public StructRef<AVCodec> codec = new StructRef<>(AVCodec.class);
    public Signed32 codec_id = new Signed32();
    public Unsigned32 codec_tag = new Unsigned32();
    public Struct.Pointer priv_data = new Pointer();
    public Struct.Pointer internal = new Pointer();
    public Struct.Pointer opaque = new Pointer();
    public int64_t bit_rate = new int64_t();
    public Signed32 bit_rate_tolerance = new Signed32();
    public Signed32 global_quality = new Signed32();
    public Signed32 compression_level = new Signed32();
    public Signed32 flags = new Signed32();
    public Signed32 flags2 = new Signed32();
    public Struct.Pointer extradata = new Pointer();
    public Signed32 extradata_size = new Signed32();
    public AVRational time_base = inner(new AVRational(getRuntime()));
    public Signed32 ticks_per_frame = new Signed32();
    public Signed32 delay = new Signed32();
    public Signed32 width = new Signed32();
    public Signed32 height = new Signed32();
    public Signed32 coded_width = new Signed32();
    public Signed32 coded_height = new Signed32();
    public Signed32 gop_size = new Signed32();
    public Enum32<AVPixelFormat> pix_fmt = new Enum32<>(AVPixelFormat.class);
    public Pointer draw_horiz_band = new Pointer();
    public Pointer get_format = new Pointer();
    public Signed32 max_b_frames = new Signed32();

    public Float b_quant_factor = new Float(); // OK
    Signed32 b_frame_strategy = new Signed32();
    public Float b_quant_offset = new Float();
    public Signed32 has_b_frames = new Signed32();
    Signed32 mpeg_quant = new Signed32();
    public Float i_quant_factor = new Float();
    public Float i_quant_offset = new Float();
    public Float lumi_masking = new Float();

    public Float temporal_cplx_masking = new Float();
    public Float spatial_cplx_masking = new Float();
    public Float p_masking = new Float();
    public Float dark_masking = new Float();
    Signed32 slice_count = new Signed32();
    Signed32 prediction_method = new Signed32();
    Pointer slice_offset = new Pointer();
    AVRational sample_aspect_ratio = inner(new AVRational(getRuntime())); // OK !

    Signed32 me_cmp = new Signed32();
    Signed32 me_sub_cmp = new Signed32();
    Signed32 mb_cmp = new Signed32();
    Signed32 ildct_cmp = new Signed32();
    Signed32 dia_size = new Signed32();
    Signed32 last_predictor_count = new Signed32();
    Signed32 pre_me = new Signed32();
    Signed32 me_pre_cmp = new Signed32();
    Signed32 pre_dia_size = new Signed32();
    Signed32 me_subpel_quality = new Signed32();
    Signed32 me_range = new Signed32();
    Signed32 slice_flags = new Signed32();
    Signed32 mb_decision = new Signed32();

    Pointer intra_matrix = new Pointer();
    Pointer inter_matrix = new Pointer();
    Signed32 scenechange_threshold = new Signed32();
    Signed32 noise_reduction = new Signed32();
    Signed32 intra_dc_precision = new Signed32();
    Signed32 skip_top = new Signed32();
    Signed32 skip_bottom = new Signed32();
    Signed32 mb_lmin = new Signed32();
    Signed32 mb_lmax = new Signed32();
    Signed32 me_penalty_compensation = new Signed32();
    Signed32 bidir_refine = new Signed32();
    Signed32 brd_scale = new Signed32();
    Signed32 keyint_min = new Signed32();
    Signed32 refs = new Signed32();
    Signed32 chromaoffset = new Signed32();
    Signed32 mv0_threshold = new Signed32();
    Signed32 b_sensitivity = new Signed32();
    Signed32 color_primaries = new Signed32();
    Signed32 color_trc = new Signed32();
    Signed32 colorspace = new Signed32();
    Signed32 color_range = new Signed32();
    Signed32 chroma_sample_location = new Signed32();
    Signed32 slices = new Signed32();
    Signed32 field_order = new Signed32(); // OK

    /* audio only */
    public Signed32 sample_rate = new Signed32();
    public Signed32 channels = new Signed32();
    public Enum32<AVSampleFormat> sample_fmt = new Enum32<>(AVSampleFormat.class);

    public Signed32 frame_size = new Signed32();
    public Signed32 frame_number = new Signed32();
    public Signed32 block_align = new Signed32();
    public Signed32 cutoff = new Signed32();

    public Unsigned64 channel_layout = new Unsigned64();
    Unsigned64 request_channel_layout = new Unsigned64();

    Signed32 audio_service_type = new Signed32();
    Signed32 request_sample_fmt = new Signed32();

    Pointer get_buffer2 = new Pointer(); // OK !

    public Signed32 refcounted_frames = new Signed32();

    public Float qcompress = new Float();
    public Float qblur = new Float();
    Signed32 qmin = new Signed32();  // OK !
    Signed32 qmax = new Signed32();  // OK !

    Signed32 max_qdiff = new Signed32();
    Signed32 rc_buffer_size = new Signed32();
    Signed32 rc_override_count = new Signed32();
    Pointer rc_override = new Pointer();
    int64_t rc_max_rate = new int64_t();
    int64_t rc_min_rate = new int64_t();
    public Float rc_max_available_vbv_use = new Float();
    public Float rc_min_vbv_overflow_use = new Float();    // 3.0 ??
    Signed32 rc_initial_buffer_occupancy = new Signed32();

    Signed32 coder_type = new Signed32();
    Signed32 context_model = new Signed32();
    Signed32 frame_skip_threshold = new Signed32();
    Signed32 frame_skip_factor = new Signed32();
    Signed32 frame_skip_exp = new Signed32();
    Signed32 frame_skip_cmp = new Signed32(); //13 ?

    Signed32 trellis = new Signed32();

    Signed32 min_prediction_order = new Signed32(); // -1
    Signed32 max_prediction_order = new Signed32(); // -1
    Signed32 timecode_frame_start = new Signed32(); // -1

    Pointer rtp_callback = new Pointer();
    Signed32 rtp_payload_size = new Signed32(); // -1

    Signed32 mv_bits = new Signed32();
    Signed32 header_bits = new Signed32();
    Signed32 i_tex_bits = new Signed32();
    Signed32 p_tex_bits = new Signed32();
    Signed32 i_count = new Signed32();
    Signed32 p_count = new Signed32();
    Signed32 skip_count = new Signed32();
    Signed32 misc_bits = new Signed32();
    Signed32 frame_bits = new Signed32();

    Pointer stats_out = new Pointer();    // BAAAAAADDDD !!!
    Pointer stats_in = new Pointer();
    Signed32 workaround_bugs = new Signed32();
    public Signed32 strict_std_compliance = new Signed32();




}