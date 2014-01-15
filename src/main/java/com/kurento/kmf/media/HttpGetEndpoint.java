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

import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.HasEndOfStreamListener;
import com.kurento.kmf.media.events.MediaSessionTerminatedEvent;

/**
 * An {@code HttpGetEndpoint} contains SOURCE pads for AUDIO and VIDEO,
 * delivering media using HTML5 pseudo-streaming mechanism.
 * <p>
 * This type of endpoint provide unidirectional communications. Its
 * {@link MediaSink} are associated with the HTTP GET method
 * </p>
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 3.0.1
 */
public interface HttpGetEndpoint extends HttpEndpoint, HasEndOfStreamListener {

	/**
	 * Builder for the {@link HttpGetEndpoint}.
	 * 
	 * @author Ivan Gracia (igracia@gsyc.es)
	 * @since 3.0.1
	 */
	public interface HttpGetEndpointBuilder extends
			MediaObjectBuilder<HttpGetEndpointBuilder, HttpGetEndpoint> {

		/**
		 * This method configures the endpoint to raise a
		 * {@link MediaSessionTerminatedEvent} when the associated player raises
		 * a {@link EndOfStreamEvent}
		 * 
		 * @return The builder
		 */
		HttpGetEndpointBuilder terminateOnEOS();

		/**
		 * Sets the disconnection timeout. This is the time that an http
		 * endpoint will wait for a reconnection, in case an HTTP connection is
		 * lost.
		 * 
		 * @param disconnectionTimeout
		 *            Time (in seconds)
		 * @return The builder
		 */
		HttpGetEndpointBuilder withDisconnectionTimeout(int disconnectionTimeout);

		/**
		 * Configures the media profile type (WEBM, MP4...) for the endpoint.
		 * 
		 * @param type
		 *            The media profile type.
		 * @return The builder
		 */
		HttpGetEndpointBuilder withMediaProfile(MediaProfileSpecType type);
	}
}
