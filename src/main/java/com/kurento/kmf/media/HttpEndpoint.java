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
 * Endpoint that enables Kurento to work as an HTTP server, allowing peer HTTP
 * clients to access media.
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

}
