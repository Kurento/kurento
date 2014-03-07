package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;

public class Point {

	private int x;
	private int y;

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
