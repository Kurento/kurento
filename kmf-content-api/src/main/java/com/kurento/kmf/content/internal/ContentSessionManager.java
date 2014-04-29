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

import java.util.concurrent.ConcurrentHashMap;

import com.kurento.kmf.content.internal.base.AbstractContentSession;

/**
 * 
 * Concurrent hash map implementation for the content request (stored using the
 * session id as key).
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class ContentSessionManager {

	/**
	 * Map to store the content requests.
	 */
	private ConcurrentHashMap<String, AbstractContentSession> requests = new ConcurrentHashMap<String, AbstractContentSession>();

	/**
	 * Default constructor.
	 */
	public ContentSessionManager() {
	}

	/**
	 * Mutator for content request concurrent hash map.
	 * 
	 * @param contentRequest
	 *            Content request to be stored in the concurrent hash map
	 * @return stored content request
	 */
	public AbstractContentSession put(AbstractContentSession contentRequest) {
		return requests.put(contentRequest.getSessionId(), contentRequest);
	}

	/**
	 * Accessor (getter) for the concurrent hash map.
	 * 
	 * @param requestId
	 *            key in the map (contentRequest.sessionId)
	 * @return found content request
	 */
	public AbstractContentSession get(String requestId) {
		return requests.get(requestId);
	}

	/**
	 * Remove an element of the concurrent hash map.
	 * 
	 * @param requestId
	 *            key in the map (contentRequest.sessionId)
	 * @return deleted content request
	 */
	public AbstractContentSession remove(String requestId) {
		return requests.remove(requestId);
	}
}
