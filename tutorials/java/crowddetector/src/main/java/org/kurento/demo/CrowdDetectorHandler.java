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

package org.kurento.demo;

import java.io.IOException;

import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * CrowdDetector with RTSP media source (application and media logic).
 *
 * @author David Fernández (d.fernandezlop@gmail.com)
 * @since 5.0.4
 */
public class CrowdDetectorHandler extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(CrowdDetectorHandler.class);
  private static final Gson gson = new GsonBuilder().create();

  @Autowired
  private Pipeline pipeline;

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

    log.debug("Incoming message: {}", jsonMessage);

    switch (jsonMessage.get("id").getAsString()) {
      case "start":
        try {
          start(session, jsonMessage);
        } catch (Throwable t) {
          sendError(session, t.getMessage());
        }
        break;

      case "stop":
        this.pipeline.removeWebRtcEndpoint(session.getId());
        break;

      case "updateFeed":
        updateFeed(jsonMessage);
        break;

      case "changeProcessingWidth":
        changeProcessingWidth(jsonMessage.get("width").getAsInt());
        break;

      case "onIceCandidate": {
        JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();

        IceCandidate cand = new IceCandidate(candidate.get("candidate").getAsString(),
            candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
        this.pipeline.addCandidate(cand, session.getId());
        break;
      }

      default:
        sendError(session, "Invalid message with id " + jsonMessage.get("id").getAsString());
        break;
    }
  }

  private void start(final WebSocketSession session, JsonObject jsonMessage) throws IOException {

    updateFeed(jsonMessage);

    if (this.pipeline.getPlayerEndpoint() == null) {
      try {
        JsonObject response = new JsonObject();
        response.addProperty("id", "noPlayer");
        session.sendMessage(new TextMessage(response.toString()));
      } catch (IOException a) {
        log.error("Exception sending message", a);
      }
      return;
    }

    if (!this.pipeline.isPlaying()) {
      try {
        JsonObject response = new JsonObject();
        response.addProperty("id", "noPlaying");
        session.sendMessage(new TextMessage(response.toString()));
      } catch (IOException a) {
        log.error("Exception sending message", a);
      }
      return;
    }

    MediaPipeline mediaPipeline = this.pipeline.getPipeline();

    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mediaPipeline).build();

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

    this.pipeline.setWebRtcEndpoint(session.getId(), webRtcEndpoint);

    this.pipeline.getCrowdDetectorFilter().connect(webRtcEndpoint);

    // SDP negotiation (offer and answer)
    String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
    String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

    // Sending response back to client
    JsonObject response = new JsonObject();
    response.addProperty("id", "startResponse");
    response.addProperty("sdpAnswer", sdpAnswer);
    String feedUrl = this.pipeline.getFeedUrl();
    log.info("Using feed URL {}", feedUrl);
    response.addProperty("feedUrl", feedUrl);
    response.addProperty("rois", gson.toJson(this.pipeline.getRois()));
    synchronized (session) {
      session.sendMessage(new TextMessage(response.toString()));
    }
    webRtcEndpoint.gatherCandidates();
  }

  private static void sendError(WebSocketSession session, String message) {
    try {
      JsonObject response = new JsonObject();
      response.addProperty("id", "error");
      response.addProperty("message", message);
      session.sendMessage(new TextMessage(response.toString()));
    } catch (IOException e) {
      log.error("Exception sending message", e);
    }
  }

  private void updateFeed(JsonObject jsonMessage) {

    JsonElement feedUrlJson = jsonMessage.get("feedUrl");
    if (feedUrlJson == null) {
      log.warn("No feed url defined");
    } else {
      String feedUrl = feedUrlJson.getAsString();
      log.debug("Updating video feed");
      this.pipeline.setFeedUrl(feedUrl);
    }
  }

  private void changeProcessingWidth(int width) {
    this.pipeline.getCrowdDetectorFilter().setProcessingWidth(width);
  }
}
