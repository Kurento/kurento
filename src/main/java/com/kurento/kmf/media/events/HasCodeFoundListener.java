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
package com.kurento.kmf.media.events;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.ListenerRegistration;
import com.kurento.kmf.media.MediaElement;

/**
 * Marker interface to indicate that a certain {@link MediaElement} can raise a
 * {@link CodeFoundEvent}, and thus listeners for this kind of event can be
 * added.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface HasCodeFoundListener {

	/**
	 * Adds a listener for {@link CodeFoundEvent}.
	 * 
	 * @param listener
	 *            The listener to be invoked when the event is raised.
	 * @return A {@link ListenerRegistration} to uniquely identify the listener
	 *         throughout the system.
	 */
	ListenerRegistration addCodeFoundDataListener(
			MediaEventListener<CodeFoundEvent> listener);

	/**
	 * Adds a listener for {@link CodeFoundEvent}.
	 * 
	 * @param listener
	 *            The listener to be invoked when the event is raised.
	 * @param cont
	 *            An asynchronous callback handler. If the event was
	 *            successfully added to the {@code HttpEndpoint}, the
	 *            {@code onSuccess} method from the handler will receive a
	 *            {@link ListenerRegistration} to uniquely identify the listener
	 *            throughout the system.
	 */
	void addCodeFoundDataListener(MediaEventListener<CodeFoundEvent> listener,
			Continuation<ListenerRegistration> cont);

}
