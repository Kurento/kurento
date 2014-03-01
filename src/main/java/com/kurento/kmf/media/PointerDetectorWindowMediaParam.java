package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;

public class PointerDetectorWindowMediaParam {

	private String id;
	private int height;
	private int width;
	private int upperRightX;
	private int upperRightY;
	private String activeImage;
	private float imageTransparency;
	private String image;
	private String inactiveImage;

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

	public String getInactiveImage() {
		return inactiveImage;
	}

	public void setInactiveImage(String inactiveImage) {
		this.inactiveImage = inactiveImage;
	}

}
