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
	 * uri of the image to be used when the pointer is inside the window
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
	 * uri of the image to be used for the window. If {@link #activeImage} has
	 * been set, it will only be shown when the pointer is outside of the
	 * window.
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

	/**
	 * 
	 * get id of the window for pointer detection
	 * 
	 **/
	public String getId() {
		return id;
	}

	/**
	 * 
	 * set id of the window for pointer detection
	 * 
	 **/
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * 
	 * get height in pixels
	 * 
	 **/
	public int getHeight() {
		return height;
	}

	/**
	 * 
	 * set height in pixels
	 * 
	 **/
	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * 
	 * get width in pixels
	 * 
	 **/
	public int getWidth() {
		return width;
	}

	/**
	 * 
	 * set width in pixels
	 * 
	 **/
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * 
	 * get X coordinate in pixels of the upper left corner
	 * 
	 **/
	public int getUpperRightX() {
		return upperRightX;
	}

	/**
	 * 
	 * set X coordinate in pixels of the upper left corner
	 * 
	 **/
	public void setUpperRightX(int upperRightX) {
		this.upperRightX = upperRightX;
	}

	/**
	 * 
	 * get Y coordinate in pixels of the upper left corner
	 * 
	 **/
	public int getUpperRightY() {
		return upperRightY;
	}

	/**
	 * 
	 * set Y coordinate in pixels of the upper left corner
	 * 
	 **/
	public void setUpperRightY(int upperRightY) {
		this.upperRightY = upperRightY;
	}

	/**
	 * 
	 * get uri of the image to be used when the pointer is inside the window
	 * 
	 **/
	public String getActiveImage() {
		return activeImage;
	}

	/**
	 * 
	 * set uri of the image to be used when the pointer is inside the window
	 * 
	 **/
	public void setActiveImage(String activeImage) {
		this.activeImage = activeImage;
	}

	/**
	 * 
	 * get transparency ratio of the image
	 * 
	 **/
	public float getImageTransparency() {
		return imageTransparency;
	}

	/**
	 * 
	 * set transparency ratio of the image
	 * 
	 **/
	public void setImageTransparency(float imageTransparency) {
		this.imageTransparency = imageTransparency;
	}

	/**
	 * 
	 * get uri of the image to be used for the window. If {@link #activeImage}
	 * has been set, it will only be shown when the pointer is outside of the
	 * window.
	 * 
	 **/
	public String getImage() {
		return image;
	}

	/**
	 * 
	 * set uri of the image to be used for the window. If {@link #activeImage}
	 * has been set, it will only be shown when the pointer is outside of the
	 * window.
	 * 
	 **/
	public void setImage(String image) {
		this.image = image;
	}

}
