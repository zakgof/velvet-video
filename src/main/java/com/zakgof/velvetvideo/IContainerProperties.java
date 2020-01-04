package com.zakgof.velvetvideo;

/**
 * Media container properties.
 */
public interface IContainerProperties {

	/**
	 * @return format name
	 */
    String format();

    /**
     * @return total duration, in nanoseconds
     */
	long nanoduration();
}