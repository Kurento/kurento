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

import java.util.Map;

import com.kurento.kmf.media.params.MediaParam;

/**
 * A mixer is a type of {@link MediaObject} that encapsulates the capability of
 * mixing (combining) different media flows. {@code MediaMixers} are useful for
 * creating applications involving group communications
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface MediaMixer extends MediaObject {

	/**
	 * Creates and endpoint in the mixer.
	 * 
	 * @return The endpoint created
	 */
	MediaElement createEndpoint();

	/**
	 * Creates and endpoint in the mixer.
	 * 
	 * @param params
	 *            Parameters to be used by the server when building the endpoint
	 * @return The endpoint created
	 */
	MediaElement createEndpoint(Map<String, MediaParam> params);

	/**
	 * Creates and endpoint in the mixer.
	 * 
	 * @param cont
	 */
	void createEndpoint(final Continuation<MediaElement> cont);

	/**
	 * Creates and endpoint in the mixer.
	 * 
	 * @param params
	 *            Parameters to be used by the server when building the endpoint
	 * @param cont
	 */
	void createEndpoint(Map<String, MediaParam> params,
			final Continuation<MediaElement> cont);

	/**
	 * Builder for the {@link MediaMixer}.
	 * 
	 * @author Ivan Gracia (igracia@gsyc.es)
	 * @since 2.0.0
	 */
	public interface MediaMixerBuilder extends
			MediaObjectBuilder<MediaMixerBuilder, MediaMixer> {

	}

}
