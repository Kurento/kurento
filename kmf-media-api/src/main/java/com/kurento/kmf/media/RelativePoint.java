/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;

/**
 * 
 * Relative points in a physical screen, values are a percentage relative to the
 * image dimensions. X left to right and Y top to down.
 * 
 **/
public class RelativePoint {

	/**
	 * 
	 * Percentage relative to the image width to calculate the X coordinate of
	 * the point [0..1]
	 * 
	 **/
	private float x;
	/**
	 * 
	 * Percentage relative to the image height to calculate the Y coordinate of
	 * the point [0..1]
	 * 
	 **/
	private float y;

	/**
	 * 
	 * Create a RelativePoint
	 * 
	 **/
	public RelativePoint(@Param("x") float x, @Param("y") float y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * 
	 * get Percentage relative to the image width to calculate the X coordinate
	 * of the point [0..1]
	 * 
	 **/
	public float getX() {
		return x;
	}

	/**
	 * 
	 * set Percentage relative to the image width to calculate the X coordinate
	 * of the point [0..1]
	 * 
	 **/
	public void setX(float x) {
		this.x = x;
	}

	/**
	 * 
	 * get Percentage relative to the image height to calculate the Y coordinate
	 * of the point [0..1]
	 * 
	 **/
	public float getY() {
		return y;
	}

	/**
	 * 
	 * set Percentage relative to the image height to calculate the Y coordinate
	 * of the point [0..1]
	 * 
	 **/
	public void setY(float y) {
		this.y = y;
	}

}
