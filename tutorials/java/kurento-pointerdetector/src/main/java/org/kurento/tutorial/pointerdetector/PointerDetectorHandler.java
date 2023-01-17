/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

package org.kurento.tutorial.pointerdetector;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.module.pointerdetector.PointerDetectorFilter;
import org.kurento.module.pointerdetector.PointerDetectorWindowMediaParam;
import org.kurento.module.pointerdetector.WindowInEvent;
import org.kurento.module.pointerdetector.WindowOutEvent;
import org.kurento.module.pointerdetector.WindowParam;
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
 * Pointer Detector handler (application and media logic).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 5.0.0
 */
public class PointerDetectorHandler extends TextWebSocketHandler {

  private final Logger log = LoggerFactory.getLogger(PointerDetectorHandler.class);
  private static final Gson gson = new GsonBuilder().create();

  private final ConcurrentHashMap<String, UserSession> users = new ConcurrentHashMap<>();

  @Autowired
  private KurentoClient kurento;

  PointerDetectorFilter pointerDetectorFilter;

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

    log.debug("Incoming message: {}", jsonMessage);

    switch (jsonMessage.get("id").getAsString()) {
      case "start":
        start(session, jsonMessage);
        break;

      case "calibrate":
        calibrate(session, jsonMessage);
        break;

      case "stop": {
        UserSession user = users.remove(session.getId());
        if (user != null) {
          user.release();
        }
        break;
      }

      case "onIceCandidate": {
        JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();

        UserSession user = users.get(session.getId());
        if (user != null) {
          IceCandidate cand = new IceCandidate(candidate.get("candidate").getAsString(),
              candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
          user.addCandidate(cand);
        }
        break;
      }

      default:
        sendError(session, "Invalid message with id " + jsonMessage.get("id").getAsString());
        break;
    }
  }

  private void calibrate(WebSocketSession session, JsonObject jsonMessage) {
    if (pointerDetectorFilter != null) {
      pointerDetectorFilter.trackColorFromCalibrationRegion();
    }
  }

  private void start(final WebSocketSession session, JsonObject jsonMessage) {
    try {
      // Media Logic (Media Pipeline and Elements)
      UserSession user = new UserSession();
      MediaPipeline pipeline = kurento.createMediaPipeline();
      user.setMediaPipeline(pipeline);
      WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
      user.setWebRtcEndpoint(webRtcEndpoint);
      users.put(session.getId(), user);

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

      pointerDetectorFilter = new PointerDetectorFilter.Builder(pipeline,
          new WindowParam(5, 5, 30, 30)).build();

      pointerDetectorFilter
      .addWindow(new PointerDetectorWindowMediaParam("window0", 50, 50, 500, 150));

      pointerDetectorFilter
      .addWindow(new PointerDetectorWindowMediaParam("window1", 50, 50, 500, 250));

      webRtcEndpoint.connect(pointerDetectorFilter);
      pointerDetectorFilter.connect(webRtcEndpoint);

      pointerDetectorFilter.addWindowInListener(new EventListener<WindowInEvent>() {
        @Override
        public void onEvent(WindowInEvent event) {
          JsonObject response = new JsonObject();
          response.addProperty("id", "windowIn");
          response.addProperty("roiId", event.getWindowId());
          try {
            session.sendMessage(new TextMessage(response.toString()));
          } catch (Throwable t) {
            sendError(session, t.getMessage());
          }
        }
      });

      pointerDetectorFilter.addWindowOutListener(new EventListener<WindowOutEvent>() {

        @Override
        public void onEvent(WindowOutEvent event) {
          JsonObject response = new JsonObject();
          response.addProperty("id", "windowOut");
          response.addProperty("roiId", event.getWindowId());
          try {
            session.sendMessage(new TextMessage(response.toString()));
          } catch (Throwable t) {
            sendError(session, t.getMessage());
          }
        }
      });

      // SDP negotiation (offer and answer)
      String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
      String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

      // Sending response back to client
      JsonObject response = new JsonObject();
      response.addProperty("id", "startResponse");
      response.addProperty("sdpAnswer", sdpAnswer);
      synchronized (session) {
        session.sendMessage(new TextMessage(response.toString()));
      }

      webRtcEndpoint.gatherCandidates();

    } catch (Throwable t) {
      sendError(session, t.getMessage());
    }
  }

  private void sendError(WebSocketSession session, String message) {
    try {
      JsonObject response = new JsonObject();
      response.addProperty("id", "error");
      response.addProperty("message", message);
      session.sendMessage(new TextMessage(response.toString()));
    } catch (IOException e) {
      log.error("Exception sending message", e);
    }
  }
}
