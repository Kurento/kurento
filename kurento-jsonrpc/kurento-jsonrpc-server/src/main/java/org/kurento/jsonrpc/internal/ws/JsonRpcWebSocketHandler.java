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

package org.kurento.jsonrpc.internal.ws;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.kurento.commons.PropertiesManager;
import org.kurento.jsonrpc.internal.client.TransactionImpl.ResponseSender;
import org.kurento.jsonrpc.internal.server.ProtocolManager;
import org.kurento.jsonrpc.internal.server.ProtocolManager.ServerSessionFactory;
import org.kurento.jsonrpc.internal.server.ServerSession;
import org.kurento.jsonrpc.internal.server.SessionsManager;
import org.kurento.jsonrpc.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class JsonRpcWebSocketHandler extends TextWebSocketHandler {

  public class MaxNumberWsConnectionsReachedException extends Exception {

    private static final long serialVersionUID = -6621614523181088993L;
  }

  private static final long MAX_WS_CONNECTIONS =
      PropertiesManager.getProperty("ws.maxSessions", Long.MAX_VALUE);

  private static final AtomicLong numConnections = new AtomicLong();

  private static final Logger log = LoggerFactory.getLogger(JsonRpcWebSocketHandler.class);

  private final ProtocolManager protocolManager;

  private String label = "";

  public JsonRpcWebSocketHandler(ProtocolManager protocolManager) {
    this.protocolManager = protocolManager;
  }

  public void setLabel(String label) {
    this.label = "[" + label + "] ";
    this.protocolManager.setLabel(label);
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {

    try {
      incNumConnectionsIfAllowed();
    } catch (MaxNumberWsConnectionsReachedException e) {
      log.warn("Closed a WS connection because MAX_WS_CONNECTIONS={} limit reached",
          MAX_WS_CONNECTIONS);
      session.close();
    }

    try {
      // We send this notification to the JsonRpcHandler when the JsonRpc
      // session is established, not when websocket session is established
      log.debug(
          "{} Client connection established from session={} uri={} headers={} acceptedProtocol={} attributes={}",
          label, session.getRemoteAddress(), session.getUri(), session.getHandshakeHeaders(),
          session.getAcceptedProtocol(), session.getAttributes());

    } catch (Throwable t) {
      log.error("{} Exception processing afterConnectionEstablished in session={}", label,
          session.getId(), t);
    }
  }

  private void incNumConnectionsIfAllowed() throws MaxNumberWsConnectionsReachedException {

    while (true) {

      long curNumConn = numConnections.get();
      if (curNumConn >= MAX_WS_CONNECTIONS) {
        throw new MaxNumberWsConnectionsReachedException();
      }

      // Try updating the value, but only if it's equal to the
      // one we've just seen. If it is different, we have to check again if now
      // there are room for a new client.
      boolean setSuccessful = numConnections.compareAndSet(curNumConn, curNumConn + 1);

      if (setSuccessful) {
        // We have incremented numConnections. Exiting.
        break;
      }

      // Another thread updated the numConnections between our get and
      // compareAndSet calls. It is possible that we check again
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status)
      throws Exception {

    numConnections.decrementAndGet();

    try {
      ServerSession session =
          (ServerSession) protocolManager.getSessionByTransportId(wsSession.getId());

      if (session != null) {

        if (session.isGracefullyClosed()) {

          log.debug("{} WebSocket session {} with transportId {} closed gracefully", label,
              session.getSessionId(), wsSession.getId());

        } else {

          log.debug(
              "{} WebSocket session {} with transportId {} closed for {} (code {}, reason '{}')",
              label, session.getSessionId(), wsSession.getId(),
              CloseStatusHelper.getCloseStatusType(status.getCode()), status.getCode(),
              status.getReason());

          protocolManager.closeSessionIfTimeout(wsSession.getId(), status.getReason());
        }
      } else {
        log.debug(
            "{} WebSocket session not associated to any jsonRpcSession "
                + "with transportId {} closed for {} (code {}, reason '{}')",
            label, wsSession.getId(), CloseStatusHelper.getCloseStatusType(status.getCode()),
            status.getCode(), status.getReason());
      }

    } catch (Throwable t) {
      log.error("{} Exception processing afterConnectionClosed in session={}", label,
          wsSession.getId(), t);
    }
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

    try {
      protocolManager.processTransportError(session.getId(), exception);
    } catch (Throwable t) {
      log.error(label + "Exception processing transportError in session={}", session.getId(), t);
    }
  }

  @Override
  public void handleTextMessage(final WebSocketSession wsSession, TextMessage message)
      throws Exception {

    try {

      String messageJson = message.getPayload();

      // TODO Ensure only one register message per websocket session.
      ServerSessionFactory factory = new ServerSessionFactory() {
        @Override
        public ServerSession createSession(String sessionId, Object registerInfo,
            SessionsManager sessionsManager) {
          return new WebSocketServerSession(sessionId, registerInfo, sessionsManager, wsSession);
        }

        @Override
        public void updateSessionOnReconnection(ServerSession session) {
          ((WebSocketServerSession) session).updateWebSocketSession(wsSession);
        }
      };

      protocolManager.processMessage(messageJson, factory, new ResponseSender() {
        @Override
        public void sendResponse(Message message) throws IOException {

          String jsonMessage = message.toString();
          log.debug("{} Res<- {}", label, jsonMessage);
          sendJsonMessage(jsonMessage);
        }

        @Override
        public void sendPingResponse(Message message) throws IOException {

          String jsonMessage = message.toString();
          log.trace("{} Res<- {}", label, jsonMessage);
          sendJsonMessage(jsonMessage);
        }

        private void sendJsonMessage(String jsonMessage) throws IOException {
          synchronized (wsSession) {
            if (wsSession.isOpen()) {
              wsSession.sendMessage(new TextMessage(jsonMessage));
            } else {
              log.error("Trying to send a message to a closed session");
            }
          }
        }
      }, wsSession.getId());

    } catch (Throwable t) {
      log.error("{} Exception processing request {}.", label, message.getPayload(), t);
    }

  }

}
