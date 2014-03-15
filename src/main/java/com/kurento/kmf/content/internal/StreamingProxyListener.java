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
package com.kurento.kmf.content.internal;

/**
 * 
 * Streaming proxy triggers events depending on the result of its operation,
 * this interfaces defines these events ( {@link #onProxySuccess},
 * {@link #onProxyError}).
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public interface StreamingProxyListener {
	/**
	 * Proxy success event declaration.
	 */
	public void onProxySuccess();

	/**
	 * Proxy error event declaration.
	 */
	public void onProxyError(String message, int errorCode);
}
