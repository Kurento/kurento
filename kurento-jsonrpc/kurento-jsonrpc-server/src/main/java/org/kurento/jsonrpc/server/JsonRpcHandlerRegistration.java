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
package org.kurento.jsonrpc.server;

import org.kurento.jsonrpc.JsonRpcHandler;

/**
 * Provides methods for configuring a JsonRpcHandler handler.
 */
public interface JsonRpcHandlerRegistration {

	/**
	 * Add more handlers that will share the same configuration
	 * 
	 * @param handler
	 *            the handler to register
	 * @param paths
	 *            paths to register the handler in
	 * @return the handler registration
	 */
	JsonRpcHandlerRegistration addHandler(JsonRpcHandler<?> handler,
			String... paths);

}