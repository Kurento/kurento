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
 * Provides function to retrieve contents from seekable sources in reliable mode
 * (does not discard media information) and inject them into KMS. It contains
 * one {@link MediaSource} for each media type detected.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface PlayerEndpoint extends UriEndpoint, HasEndOfStreamListener {

	/**
	 * Starts to send data to the endpoint's {@link MediaSource}
	 */
	void play();

	/**
	 * Starts to send data to the endpoint's {@link MediaSource}
	 * 
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked in case of correct
	 *            connection, but no extra information will be provided.
	 */
	void play(Continuation<Void> cont);

	/**
	 * Builder for the {@link PlayerEndpoint}.
	 *
	 * @author Ivan Gracia (igracia@gsyc.es)
	 * @since 2.0.0
	 */
	public interface PlayerEndpointBuilder extends
			MediaObjectBuilder<PlayerEndpointBuilder, PlayerEndpoint> {

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
		PlayerEndpointBuilder useEncodedMedia();

	}

}
