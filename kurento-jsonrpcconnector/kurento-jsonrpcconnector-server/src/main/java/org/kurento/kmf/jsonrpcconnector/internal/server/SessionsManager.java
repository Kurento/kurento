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
package org.kurento.kmf.jsonrpcconnector.internal.server;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 
 * This class is responsible for storing JSON-RPC sessions using a
 * ConcurrentHashMap.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @version 1.0.0
 */
@Component
public class SessionsManager {

	// TODO Review atomic management of two maps

	private final ConcurrentHashMap<String, ServerSession> sessions = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, ServerSession> sessionsByTransportId = new ConcurrentHashMap<>();

	public void put(ServerSession session) {
		sessions.put(session.getSessionId(), session);
		String transportId = session.getTransportId();
		if (transportId != null) {
			sessionsByTransportId.put(transportId, session);
		}
	}

	public ServerSession get(String sessionId) {
		return sessions.get(sessionId);
	}

	public ServerSession getByTransportId(String transportId) {
		return sessionsByTransportId.get(transportId);
	}

	public ServerSession removeByTransportId(String transportId) {
		ServerSession session = sessionsByTransportId.remove(transportId);
		if (session != null) {
			sessions.remove(session.getSessionId());
		}
		return session;
	}

	public ServerSession remove(String sessionId) {
		ServerSession session = sessions.remove(sessionId);
		if (session != null) {
			sessionsByTransportId.remove(session.getTransportId());
		}
		return session;
	}

	public void updateTransportId(ServerSession session, String oldTransportId) {
		if (oldTransportId != null) {
			sessionsByTransportId.remove(oldTransportId);
		}

		if (session.getTransportId() != null) {
			sessionsByTransportId.put(session.getTransportId(), session);
		}
	}

	public void remove(ServerSession session) {
		remove(session.getSessionId());
	}
}
