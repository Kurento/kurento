/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;

/**
 * 
 * Point in a physical screen, coordinates are in pixels with X left to right
 * and Y top to down.
 * 
 **/
public class Point {

	/**
	 * 
	 * X coordinate in pixels of a point in the screen
	 * 
	 **/
	private int x;
	/**
	 * 
	 * Y coordinate in pixels of a point in the screen
	 * 
	 **/
	private int y;

	/**
	 * 
	 * Create a Point
	 * 
	 **/
	public Point(@Param("x") int x, @Param("y") int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

}
