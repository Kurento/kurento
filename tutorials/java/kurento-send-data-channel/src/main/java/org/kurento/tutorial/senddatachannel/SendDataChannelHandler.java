/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

package org.kurento.tutorial.senddatachannel;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.module.datachannelexample.KmsSendData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Show Data Channel handler (application and media logic).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 6.1.1
 */
public class SendDataChannelHandler extends TextWebSocketHandler {

  private final Logger log = LoggerFactory.getLogger(SendDataChannelHandler.class);
  private static final Gson gson = new GsonBuilder().create();

  private final ConcurrentHashMap<String, UserSession> users = new ConcurrentHashMap<>();

  @Autowired
  private KurentoClient kurento;

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

    log.debug("Incoming message: {}", jsonMessage);

    switch (jsonMessage.get("id").getAsString()) {
    case "start":
      start(session, jsonMessage);
      break;
    case "stop": {
      UserSession user = users.remove(session.getId());
      if (user != null) {
        user.release();
      }
      break;
    }
    case "onIceCandidate": {
      JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();

      UserSession user = users.get(session.getId());
      if (user != null) {
        IceCandidate candidate = new IceCandidate(jsonCandidate.get("candidate").getAsString(),
            jsonCandidate.get("sdpMid").getAsString(),
            jsonCandidate.get("sdpMLineIndex").getAsInt());
        user.addCandidate(candidate);
      }
      break;
    }
    default:
      sendError(session, "Invalid message with id " + jsonMessage.get("id").getAsString());
      break;
    }
  }

  private void start(final WebSocketSession session, JsonObject jsonMessage) {
    try {
      // User session
      UserSession user = new UserSession();
      MediaPipeline pipeline = kurento.createMediaPipeline();
      user.setMediaPipeline(pipeline);
      WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).useDataChannels()
          .build();
      user.setWebRtcEndpoint(webRtcEndpoint);
      PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
          "http://files.openvidu.io/video/filter/barcodes.webm").build();
      user.setPlayer(player);
      users.put(session.getId(), user);

      player.addErrorListener(new EventListener<ErrorEvent>() {
        @Override
        public void onEvent(ErrorEvent event) {
          log.info("ErrorEvent: {}", event.getDescription());
          sendPlayEnd(session);
        }
      });

      player.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
        @Override
        public void onEvent(EndOfStreamEvent event) {
          log.info("EndOfStreamEvent: {}", event.getTimestampMillis());
          sendPlayEnd(session);
        }
      });

      // ICE candidates
      webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
        @Override
        public void onEvent(IceCandidateFoundEvent event) {
          JsonObject response = new JsonObject();
          response.addProperty("id", "iceCandidate");
          response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
          try {
            synchronized (session) {
              session.sendMessage(new TextMessage(response.toString()));
            }
          } catch (IOException e) {
            log.debug(e.getMessage());
          }
        }
      });

      // Media logic
      KmsSendData kmsSendData = new KmsSendData.Builder(pipeline).build();

      player.connect(kmsSendData);
      kmsSendData.connect(webRtcEndpoint);

      // SDP negotiation (offer and answer)
      String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
      String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

      JsonObject response = new JsonObject();
      response.addProperty("id", "startResponse");
      response.addProperty("sdpAnswer", sdpAnswer);

      synchronized (session) {
        session.sendMessage(new TextMessage(response.toString()));
      }

      webRtcEndpoint.gatherCandidates();
      player.play();

    } catch (Throwable t) {
      sendError(session, t.getMessage());
    }
  }

  private void sendError(WebSocketSession session, String message) {
    if (users.containsKey(session.getId())) {
      JsonObject response = new JsonObject();
      response.addProperty("id", "error");
      response.addProperty("message", message);
      sendMessage(session, response.toString());
    }
  }

  public void sendPlayEnd(WebSocketSession session) {
    if (users.containsKey(session.getId())) {
      JsonObject response = new JsonObject();
      response.addProperty("id", "playEnd");
      sendMessage(session, response.toString());
    }
  }

  private synchronized void sendMessage(WebSocketSession session, String message) {
    try {
      session.sendMessage(new TextMessage(message));
    } catch (IOException e) {
      log.error("Exception sending message", e);
    }
  }
}
