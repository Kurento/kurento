/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.media.params.internal;

/**
 * Class that represetns a window, to be used in command and constructor for
 * media elements.
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 3.0.1
 * 
 */
public class WindowParam {

	private final int topRightCornerX;

	private final int topRightCornerY;

	private final int width;

	private final int height;

	public WindowParam(int topRightCornerX, int topRightCornerY, int width,
			int height) {
		this.topRightCornerX = topRightCornerX;
		this.topRightCornerY = topRightCornerY;
		this.width = width;
		this.height = height;
	}

	public int getUpperRightX() {
		return this.topRightCornerX;
	}

	public int getUpperRightY() {
		return this.topRightCornerY;
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	@Override
	public boolean equals(Object obj) {

		if (obj == null) {
			return false;
		}

		if (obj == this || this.getClass() != (obj.getClass())) {
			return true;
		}

		final WindowParam window = (WindowParam) obj;

		boolean ret = this.height == window.height;
		ret &= this.width == window.width;
		ret &= this.topRightCornerX == window.topRightCornerX;
		ret &= this.topRightCornerY == window.topRightCornerY;

		return ret;
	}

	@Override
	public int hashCode() {
		int result = 13;
		result = (result * 31 + this.height);
		result = (result * 31 + this.width);
		result = (result * 31 + this.topRightCornerX);
		result = (result * 31 + this.topRightCornerY);
		return result;
	}

}
