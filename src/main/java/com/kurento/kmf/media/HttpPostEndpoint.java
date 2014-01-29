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

import com.kurento.kmf.media.events.HasEndOfStreamListener;

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
public interface HttpPostEndpoint extends HttpEndpoint, HasEndOfStreamListener {

	/**
	 * Builder for the {@link HttpPostEndpoint}.
	 *
	 * @author Ivan Gracia (igracia@gsyc.es)
	 * @since 3.0.1
	 */
	public interface HttpPostEndpointBuilder extends
			MediaObjectBuilder<HttpPostEndpointBuilder, HttpPostEndpoint> {

		/**
		 * This method configures the endpoint to use encoded
		 * media instead of raw media. If the parameter is not set then
		 * the element uses raw media. Changing this parameter could affect in
		 * a severe way to stability because key frames lost will not be
		 * generated. Changing the media type does not affect to the
		 * result except in the performance (just in the case where original
		 * media and target media are the same) and in the problem with the
		 * key frames. We strongly recommended not to use this parameter
		 * because correct behaviour is not guarantied.
		 *
		 * @return The builder
		 */
		HttpPostEndpointBuilder useEncodedMedia();

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
