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
 * Provides function to store contents in reliable mode (doesn't discard data).
 * It contains {@link MediaSink} pads for audio and video.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface RecorderEndpoint extends UriEndpoint {

	/**
	 * Starts storing media received through the {@link MediaSink} pad
	 */
	void record();

	/**
	 * Starts storing media received through the {@link MediaSink} pad
	 * 
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked in case of correct
	 *            invocation, but no extra information will be provided.
	 */
	void record(Continuation<Void> cont);

	/**
	 * Builder for the {@link ZBarFilter}.
	 * 
	 * @author Ivan Gracia (igracia@gsyc.es)
	 * @since 2.0.0
	 */
	public interface RecorderEndpointBuilder extends
			MediaObjectBuilder<RecorderEndpointBuilder, RecorderEndpoint> {

		RecorderEndpointBuilder withMediaProfile(MediaProfileSpecType type);

		/**
		 * Forces the recorder end point to finish processing data when an EOS
		 * is detected in the stream.
		 * 
		 * @return The builder
		 */
		RecorderEndpointBuilder stopOnEndOfStream();

	}

}
