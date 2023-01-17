/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.jsonrpc.internal.server;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * This class is responsible for storing JSON-RPC sessions using a ConcurrentHashMap.
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @version 1.0.0
 */
@Component
public class SessionsManager {

  // TODO Review atomic management of two maps

  private static Logger log = LoggerFactory.getLogger(SessionsManager.class);

  private final ConcurrentHashMap<String, ServerSession> sessions = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, ServerSession> sessionsByTransportId = new ConcurrentHashMap<>();

  public void put(ServerSession session) {

    sessions.put(session.getSessionId(), session);

    String transportId = session.getTransportId();

    if (transportId != null) {
      sessionsByTransportId.put(transportId, session);
    } else {
      log.warn("Session {} has not transportId associated to it", session.getSessionId());
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

  @Override
  public String toString() {
    return "SessionsManager [sessions=" + sessions + ", sessionsByTransportId="
        + sessionsByTransportId + "]";
  }

}
