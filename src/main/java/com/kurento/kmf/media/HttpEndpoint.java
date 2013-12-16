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
import com.kurento.kmf.media.events.MediaSessionTerminatedEvent;

/**
 * Endpoint that enables Kurento to work as an HTTP server, allowing peer HTTP
 * clients to access media. An {@code HttpEndpoint} contains both SINK and
 * SOURCE pads for AUDIO and VIDEO. SINK elements provide access to an HTTP file
 * upload function while SOURCE delivers media using HTML5 pseudostreaming
 * mechanism.
 * <p>
 * This type of endpoint provide bidirectional communications. Its
 * {@link MediaSink} are associated with the HTTP GET method, while
 * {@link MediaSource} are related to HTTP POST method.
 * </p>
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface HttpEndpoint extends SessionEndpoint {

	/**
	 * Obtains the URL associated to this endpoint
	 * 
	 * @return The url
	 */
	String getUrl();

	/**
	 * Obtains the URL associated to this endpoint
	 * 
	 * @param cont
	 *            An asynchronous callback handler. If the command was invoked
	 *            successfully on the {@code HttpEndpoint}, the
	 *            {@code onSuccess} method from the handler will receive a
	 *            {@code String} representing the URL.
	 */
	void getUrl(Continuation<String> cont);

	/**
	 * Builder for the {@link HttpEndpoint}.
	 * 
	 * @author Ivan Gracia (igracia@gsyc.es)
	 * @since 2.0.0
	 */
	public interface HttpEndpointBuilder extends
			MediaObjectBuilder<HttpEndpointBuilder, HttpEndpoint> {

		/**
		 * This method configures the endpoint to raise a
		 * {@link MediaSessionTerminatedEvent} when the associated player raises
		 * a {@link EndOfStreamEvent}
		 * 
		 * @return The builder
		 */
		HttpEndpointBuilder terminateOnEOS();

		/**
		 * Sets the disconnection timeout. This is the time that an http
		 * endpoint will wait for a reconnection, in case an HTTP connection is
		 * lost.
		 * 
		 * @param disconnectionTimeout
		 *            Time (in seconds)
		 * @return The builder
		 */
		HttpEndpointBuilder withDisconnectionTimeout(int disconnectionTimeout);

		/**
		 * Configures the media profile type (WEBM, MP4...) for the endpoint.
		 * 
		 * @param type
		 *            The media profile tpye.
		 * @return The builder
		 */
		HttpEndpointBuilder withMediaProfile(MediaProfileSpecType type);
	}
}
