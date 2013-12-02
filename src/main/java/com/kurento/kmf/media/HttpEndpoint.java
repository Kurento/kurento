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

import com.kurento.kmf.media.events.HttpEndpointEOSDetected;
import com.kurento.kmf.media.events.MediaEventListener;

public interface HttpEndpoint extends SessionEndpoint {

	/* SYNC */
	String getUrl();

	ListenerRegistration addEOSDetectedListener(
			final MediaEventListener<HttpEndpointEOSDetected> sessionEvent);

	/* ASYNC */

	void getUrl(final Continuation<String> cont);

	void addEOSDetectedListener(
			final MediaEventListener<HttpEndpointEOSDetected> sessionEvent,
			final Continuation<ListenerRegistration> cont);

	public interface HttpEndpointBuilder extends
			MediaObjectBuilder<HttpEndpointBuilder, HttpEndpoint> {

		HttpEndpointBuilder terminateOnEOS();

		HttpEndpointBuilder withDisconnectionTimeout(int disconnectionTimeout);

		HttpEndpointBuilder withMediaProfile(MediaProfileSpecType type);
	}
}
