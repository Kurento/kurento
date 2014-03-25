/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;

/**
 * 
 * Data structure for UI Pointer detection in video streams. All the coordinates
 * are in pixels. X is horizontal, Y is vertical, running from the top of the
 * window. Thus, 0,0 corresponds to the topleft corner.
 * 
 **/
public class PointerDetectorWindowMediaParam {

	/**
	 * 
	 * id of the window for pointer detection
	 * 
	 **/
	private String id;
	/**
	 * 
	 * height in pixels
	 * 
	 **/
	private int height;
	/**
	 * 
	 * width in pixels
	 * 
	 **/
	private int width;
	/**
	 * 
	 * X coordinate in pixels of the upper left corner
	 * 
	 **/
	private int upperRightX;
	/**
	 * 
	 * Y coordinate in pixels of the upper left corner
	 * 
	 **/
	private int upperRightY;
	/**
	 * 
	 * uri of the image (cursor) to be used to indicate activity
	 * 
	 **/
	private String activeImage;
	/**
	 * 
	 * transparency ratio of the image
	 * 
	 **/
	private float imageTransparency;
	/**
	 * 
	 * uri of the image (cursor) to be used when not active
	 * 
	 **/
	private String image;

	/**
	 * 
	 * Create a PointerDetectorWindowMediaParam
	 * 
	 **/
	public PointerDetectorWindowMediaParam(@Param("id") String id,
			@Param("height") int height, @Param("width") int width,
			@Param("upperRightX") int upperRightX,
			@Param("upperRightY") int upperRightY) {
		this.id = id;
		this.height = height;
		this.width = width;
		this.upperRightX = upperRightX;
		this.upperRightY = upperRightY;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getUpperRightX() {
		return upperRightX;
	}

	public void setUpperRightX(int upperRightX) {
		this.upperRightX = upperRightX;
	}

	public int getUpperRightY() {
		return upperRightY;
	}

	public void setUpperRightY(int upperRightY) {
		this.upperRightY = upperRightY;
	}

	public String getActiveImage() {
		return activeImage;
	}

	public void setActiveImage(String activeImage) {
		this.activeImage = activeImage;
	}

	public float getImageTransparency() {
		return imageTransparency;
	}

	public void setImageTransparency(float imageTransparency) {
		this.imageTransparency = imageTransparency;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

}
