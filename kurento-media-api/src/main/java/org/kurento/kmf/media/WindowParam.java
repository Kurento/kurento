/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.kmf.media;

import org.kurento.tool.rom.server.Param;

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

	/**
	 * 
	 * get X coordinate of the left upper point of the window
	 * 
	 **/
	public int getTopRightCornerX() {
		return topRightCornerX;
	}

	/**
	 * 
	 * set X coordinate of the left upper point of the window
	 * 
	 **/
	public void setTopRightCornerX(int topRightCornerX) {
		this.topRightCornerX = topRightCornerX;
	}

	/**
	 * 
	 * get Y coordinate of the left upper point of the window
	 * 
	 **/
	public int getTopRightCornerY() {
		return topRightCornerY;
	}

	/**
	 * 
	 * set Y coordinate of the left upper point of the window
	 * 
	 **/
	public void setTopRightCornerY(int topRightCornerY) {
		this.topRightCornerY = topRightCornerY;
	}

	/**
	 * 
	 * get width in pixels of the window
	 * 
	 **/
	public int getWidth() {
		return width;
	}

	/**
	 * 
	 * set width in pixels of the window
	 * 
	 **/
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * 
	 * get height in pixels of the window
	 * 
	 **/
	public int getHeight() {
		return height;
	}

	/**
	 * 
	 * set height in pixels of the window
	 * 
	 **/
	public void setHeight(int height) {
		this.height = height;
	}

}
