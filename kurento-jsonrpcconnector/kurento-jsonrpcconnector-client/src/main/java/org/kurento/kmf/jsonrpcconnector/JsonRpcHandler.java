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
package org.kurento.kmf.jsonrpcconnector;

import org.kurento.kmf.jsonrpcconnector.internal.message.Request;

public interface JsonRpcHandler<P> {

	/**
	 * Invoked when a new JsonRpc request arrives.
	 * 
	 * @param transaction
	 *            the transaction to which the request belongs
	 * @param request
	 *            the request
	 * 
	 * @throws TransportException
	 *             when there is an error in the transport mechanism
	 * 
	 * @throws Exception
	 *             this method can handle or propagate exceptions.
	 */
	void handleRequest(Transaction transaction, Request<P> request)
			throws Exception;

	void afterConnectionEstablished(Session session) throws Exception;

	void afterConnectionClosed(Session session, String status) throws Exception;

	void handleTransportError(Session session, Throwable exception)
			throws Exception;

	void handleUncaughtException(Session session, Exception exception);

	Class<?> getHandlerType();
}
