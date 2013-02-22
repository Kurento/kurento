/*
 * Kurento Commons MSControl: Simplified Media Control API for the Java Platform based on jsr309
 * Copyright (C) 2012  Tikal Technologies
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.kurento.kmf.media;


/**
 * A MediaSink receives media from a connected MediaSrc (if any)
 * 
 */
public interface MediaSink extends MediaResource {

	/**
	 * Returns the Joined MediaSrc or null if not joined
	 * 
	 * @return The joined MediaSrc or null if not joined
	 */
	public MediaSrc getConnectedSrc();
}
