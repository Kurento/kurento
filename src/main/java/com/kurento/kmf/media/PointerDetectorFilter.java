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
import com.kurento.kmf.media.params.internal.PointerDetectorWindowMediaParam;

/**
 * PointerDetectorFilter interface. This type of {@code Endpoint} detects
 * pointers in a video feed.
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.1
 */
public interface PointerDetectorFilter extends Filter, HasWindowOutListener,
		HasWindowInListener {

	/**
	 * Adds a pointer detector window. When a pointer enters or exits this
	 * window, the filter will raise an event indicating so.
	 * 
	 * @param window
	 */
	void addWindow(PointerDetectorWindowMediaParam window);

	/**
	 * Removes a window
	 * 
	 * @param windowId
	 *            the id of the window
	 */
	void removeWindow(String windowId);

	/**
	 * Removes all pointer detector windows
	 */
	void clearWindows();

	/**
	 * Adds a pointer detector window. When a pointer enters or exits this
	 * window, the filter will raise an event indicating so.
	 * 
	 * @param window
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked in case of correct
	 *            connection, but no extra information will be provided.
	 */
	void addWindow(PointerDetectorWindowMediaParam window,
			Continuation<Void> cont);

	/**
	 * Removes a window
	 * 
	 * @param windowId
	 *            the id of the window
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked in case of correct
	 *            connection, but no extra information will be provided.
	 */
	void removeWindow(String windowId, Continuation<Void> cont);

	/**
	 * Removes all pointer detector windows
	 * 
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked in case of correct
	 *            connection, but no extra information will be provided.
	 */
	void clearWindows(Continuation<Void> cont);

	/**
	 * Builder for the {@link PointerDetectorFilter}.
	 * 
	 * @author Ivan Gracia (igracia@gsyc.es)
	 * @since 2.0.1
	 */
	public interface PointerDetectorFilterBuilder
			extends
			MediaObjectBuilder<PointerDetectorFilterBuilder, PointerDetectorFilter> {

		/**
		 * Adds a new detection window for the filter to detect pointers
		 * entering or exiting the window.
		 * 
		 * @param window
		 *            The window
		 * @return The builder.
		 */
		PointerDetectorFilterBuilder withWindow(
				PointerDetectorWindowMediaParam window);

	}

}
