package com.zakgof.velvetvideo;

/**
 * Exception class for all velvet-video errors.
 */
public class VelvetVideoException extends RuntimeException {

	private static final long serialVersionUID = -358651198038761511L;

	public VelvetVideoException(String message) {
		super(message);
	}

	public VelvetVideoException(Throwable causedBy) {
		super(causedBy);
	}

	public VelvetVideoException(String message, Throwable causedBy) {
		super(message, causedBy);
	}
}