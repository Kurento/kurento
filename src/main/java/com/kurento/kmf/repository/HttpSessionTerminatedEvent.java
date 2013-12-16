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

package com.kurento.kmf.repository;

/**
 * This class represents an event fired when an client is "considered"
 * disconnected for the {@link RepositoryHttpEndpoint} identified as source.
 * 
 * The {@link RepositoryHttpEndpoint} is based on http protocol. As this
 * protocol is stateless, there is no concept of "connection". For this reason,
 * the way to consider that a client is disconnected is when a time is elapsed
 * without requests for the client. This concept is commonly used to manage http
 * sessions in web applications. The timeout can be configured in the endpoint
 * with {@link RepositoryHttpEndpoint#setAutoTerminationTimeout(long)}
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 */
public class HttpSessionTerminatedEvent extends RepositoryHttpSessionEvent {

	public HttpSessionTerminatedEvent(RepositoryHttpEndpoint source) {
		super(source);
	}

}
