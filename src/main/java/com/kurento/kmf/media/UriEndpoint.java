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
 * Interface for endpoints the require a URI to work. An example of this, would
 * be a {@link PlayerEndpoint}, whose URI property could be used to locate a
 * file to stream through is {@link MediaSource}
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface UriEndpoint extends Endpoint {

	/**
	 * Returns the uri for this endpoint.
	 * 
	 * @return The uri
	 */
	String getUri();

	/**
	 * Pauses the feed.
	 */
	void pause();

	/**
	 * Stops the feed
	 */
	void stop();

	/**
	 * Returns the uri for this endpoint.
	 * 
	 * @param cont
	 *            The callback function to be invoked asynchronously
	 */
	void getUri(Continuation<String> cont);

	/**
	 * Pauses the feed
	 * 
	 * @param cont
	 *            The callback function to be invoked asynchronously
	 */
	void pause(Continuation<Void> cont);

	/**
	 * Stops the feed
	 * 
	 * @param cont
	 *            The callback function to be invoked asynchronously
	 */
	void stop(Continuation<Void> cont);

}
