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
package com.kurento.kmf.media;

import com.kurento.kmf.media.events.HasWindowInListener;
import com.kurento.kmf.media.events.HasWindowOutListener;

/**
 * PointerDetectorFilter interface. This type of {@code Endpoint} detects
 * pointers in a video feed.
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */

public interface PointerDetectorFilter extends Filter, HasWindowOutListener,
		HasWindowInListener {

	void addWindow(int upperRightX, int upperRightY, int width, int height,
			String id);

	void removeWindow(String windowId);

	void clearWindows();

	void addWindow(int upperRightX, int upperRightY, int width, int height,
			String id, Continuation<Void> cont);

	void removeWindow(String windowId, Continuation<Void> cont);

	void clearWindows(Continuation<Void> cont);

	/**
	 * Builder for the {@link PointerDetectorFilter}.
	 * 
	 * @author Ivan Gracia (igracia@gsyc.es)
	 * @since 2.0.0
	 */
	public interface PointerDetectorFilterBuilder
			extends
			MediaObjectBuilder<PointerDetectorFilterBuilder, PointerDetectorFilter> {

		/**
		 * Adds a new detection window for the filter to detect pointers
		 * entering or exiting the window.
		 * 
		 * @param windowId
		 *            Id to manage the window once the endpoint is created.
		 *            Events generated in this window also use this id.
		 * @param height
		 *            Window height.
		 * @param width
		 *            Window width.
		 * @param upperRightX
		 *            Upper right corner X coordinate.
		 * @param upperRightY
		 *            Upper right corner Y coordinate.
		 * @return The builder.
		 */
		PointerDetectorFilterBuilder withWindow(String windowId, int height,
				int width, int upperRightX, int upperRightY);
	}

}
