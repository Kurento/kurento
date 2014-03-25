/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;

/**
 * 
 * Parameter representing a window in a video stream. It is used in command and
 * constructors for media elements. All units are in pixels, X runs from left to
 * right, Y from top to bottom.
 * 
 **/
public class WindowParam {

	/**
	 * 
	 * X coordinate of the left upper point of the window
	 * 
	 **/
	private int topRightCornerX;
	/**
	 * 
	 * Y coordinate of the left upper point of the window
	 * 
	 **/
	private int topRightCornerY;
	/**
	 * 
	 * width in pixels of the window
	 * 
	 **/
	private int width;
	/**
	 * 
	 * height in pixels of the window
	 * 
	 **/
	private int height;

	/**
	 * 
	 * Create a WindowParam
	 * 
	 **/
	public WindowParam(@Param("topRightCornerX") int topRightCornerX,
			@Param("topRightCornerY") int topRightCornerY,
			@Param("width") int width, @Param("height") int height) {
		this.topRightCornerX = topRightCornerX;
		this.topRightCornerY = topRightCornerY;
		this.width = width;
		this.height = height;
	}

	public int getTopRightCornerX() {
		return topRightCornerX;
	}

	public void setTopRightCornerX(int topRightCornerX) {
		this.topRightCornerX = topRightCornerX;
	}

	public int getTopRightCornerY() {
		return topRightCornerY;
	}

	public void setTopRightCornerY(int topRightCornerY) {
		this.topRightCornerY = topRightCornerY;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

}
