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

/**
 * An {@code HttpPostEndpoint} contains SINK pads for AUDIO and VIDEO, which
 * provide access to an HTTP file upload function
 * <p>
 * This type of endpoint provide unidirectional communications. Its
 * {@link MediaSource} are related to HTTP POST method.
 * </p>
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 3.0.1
 */
public interface HttpPostEndpoint extends HttpEndpoint {

	/**
	 * Builder for the {@link HttpPostEndpoint}.
	 * 
	 * @author Ivan Gracia (igracia@gsyc.es)
	 * @since 3.0.1
	 */
	public interface HttpPostEndpointBuilder extends
			MediaObjectBuilder<HttpPostEndpointBuilder, HttpPostEndpoint> {

		/**
		 * Sets the disconnection timeout. This is the time that an http
		 * endpoint will wait for a reconnection, in case an HTTP connection is
		 * lost.
		 * 
		 * @param disconnectionTimeout
		 *            Time (in seconds)
		 * @return The builder
		 */
		HttpPostEndpointBuilder withDisconnectionTimeout(
				int disconnectionTimeout);
	}
}
