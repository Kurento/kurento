/*
 * (C) Copyright 2015-2016 Kurento (http://kurento.org/)
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
 */
package org.kurento.tutorial.helloworld;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.repository.RepositoryClient;
import org.kurento.repository.service.pojo.RepositoryItemPlayer;
import org.kurento.repository.service.pojo.RepositoryItemRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Recording in repository handler (application and media logic).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.1.1
 */
public class HelloWorldRecHandler extends TextWebSocketHandler {

  // slightly larger timeout
  private static final int REPOSITORY_DISCONNECT_TIMEOUT = 5500;

  private static final String RECORDING_EXT = ".webm";

  private final Logger log = LoggerFactory.getLogger(HelloWorldRecHandler.class);
  private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-S");
  private final Gson gson = new GsonBuilder().create();

  @Autowired
  private UserRegistry registry;

  @Autowired
  private KurentoClient kurento;

  @Autowired
  private RepositoryClient repositoryClient;

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

    log.debug("Incoming message: {}", jsonMessage);

    UserSession user = registry.getBySession(session);
    if (user != null) {
      log.debug("Incoming message from user '{}': {}", user.getId(), jsonMessage);
    } else {
      log.debug("Incoming message from new user: {}", jsonMessage);
    }

    switch (jsonMessage.get("id").getAsString()) {
      case "start":
        start(session, jsonMessage);
        break;
      case "stop":
      case "stopPlay":
        if (user != null) {
          user.release();
        }
        break;
      case "play":
        play(user, session, jsonMessage);
        break;
      case "onIceCandidate": {
        JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();

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

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    super.afterConnectionClosed(session, status);
    registry.removeBySession(session);
  }

  private void start(final WebSocketSession session, JsonObject jsonMessage) {
    try {
      // 0. Repository logic
      RepositoryItemRecorder repoItem = null;
      if (repositoryClient != null) {
        try {
          Map<String, String> metadata = Collections.emptyMap();
          repoItem = repositoryClient.createRepositoryItem(metadata);
        } catch (Exception e) {
          log.warn("Unable to create kurento repository items", e);
        }
      } else {
        String now = df.format(new Date());
        String filePath = HelloWorldRecApp.REPOSITORY_SERVER_URI + now + RECORDING_EXT;
        repoItem = new RepositoryItemRecorder();
        repoItem.setId(now);
        repoItem.setUrl(filePath);
      }
      log.info("Media will be recorded {}by KMS: id={} , url={}",
          (repositoryClient == null ? "locally " : ""), repoItem.getId(), repoItem.getUrl());

      // 1. Media logic (webRtcEndpoint in loopback)
      MediaPipeline pipeline = kurento.createMediaPipeline();
      WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
      webRtcEndpoint.connect(webRtcEndpoint);
      RecorderEndpoint recorder = new RecorderEndpoint.Builder(pipeline, repoItem.getUrl())
      .withMediaProfile(MediaProfileSpecType.WEBM).build();
      webRtcEndpoint.connect(recorder);

      // 2. Store user session
      UserSession user = new UserSession(session);
      user.setMediaPipeline(pipeline);
      user.setWebRtcEndpoint(webRtcEndpoint);
      user.setRepoItem(repoItem);
      registry.register(user);

      // 3. SDP negotiation
      String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
      String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

      // 4. Gather ICE candidates
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
            log.error(e.getMessage());
          }
        }
      });

      JsonObject response = new JsonObject();
      response.addProperty("id", "startResponse");
      response.addProperty("sdpAnswer", sdpAnswer);

      synchronized (user) {
        session.sendMessage(new TextMessage(response.toString()));
      }

      webRtcEndpoint.gatherCandidates();

      recorder.record();
    } catch (Throwable t) {
      log.error("Start error", t);
      sendError(session, t.getMessage());
    }
  }

  private void play(UserSession user, final WebSocketSession session, JsonObject jsonMessage) {
    try {
      // 0. Repository logic
      RepositoryItemPlayer itemPlayer = null;
      if (repositoryClient != null) {
        try {
          Date stopTimestamp = user.getStopTimestamp();
          if (stopTimestamp != null) {
            Date now = new Date();
            long diff = now.getTime() - stopTimestamp.getTime();
            if (diff >= 0 && diff < REPOSITORY_DISCONNECT_TIMEOUT) {
              log.info(
                  "Waiting for {}ms before requesting the repository read endpoint "
                      + "(requires {}ms before upload is considered terminated "
                      + "and only {}ms have passed)",
                      REPOSITORY_DISCONNECT_TIMEOUT - diff, REPOSITORY_DISCONNECT_TIMEOUT, diff);
              Thread.sleep(REPOSITORY_DISCONNECT_TIMEOUT - diff);
            }
          } else {
            log.warn("No stop timeout was found, repository endpoint might not be ready");
          }
          itemPlayer = repositoryClient.getReadEndpoint(user.getRepoItem().getId());
        } catch (Exception e) {
          log.warn("Unable to obtain kurento repository endpoint", e);
        }
      } else {
        itemPlayer = new RepositoryItemPlayer();
        itemPlayer.setId(user.getRepoItem().getId());
        itemPlayer.setUrl(user.getRepoItem().getUrl());
      }
      log.debug("Playing from {}: id={}, url={}",
          (repositoryClient == null ? "disk" : "repository"), itemPlayer.getId(),
          itemPlayer.getUrl());

      // 1. Media logic
      final MediaPipeline pipeline = kurento.createMediaPipeline();
      WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
      PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline, itemPlayer.getUrl()).build();
      player.connect(webRtcEndpoint);

      // Player listeners
      player.addErrorListener(new EventListener<ErrorEvent>() {
        @Override
        public void onEvent(ErrorEvent event) {
          log.info("ErrorEvent for session '{}': {}", session.getId(), event.getDescription());
          sendPlayEnd(session, pipeline);
        }
      });
      player.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
        @Override
        public void onEvent(EndOfStreamEvent event) {
          log.info("EndOfStreamEvent for session '{}'", session.getId());
          sendPlayEnd(session, pipeline);
        }
      });

      // 2. Store user session
      user.setMediaPipeline(pipeline);
      user.setWebRtcEndpoint(webRtcEndpoint);

      // 3. SDP negotiation
      String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
      String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

      JsonObject response = new JsonObject();
      response.addProperty("id", "playResponse");
      response.addProperty("sdpAnswer", sdpAnswer);

      // 4. Gather ICE candidates
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
            log.error(e.getMessage());
          }
        }
      });

      // 5. Play recorded stream
      player.play();

      synchronized (session) {
        session.sendMessage(new TextMessage(response.toString()));
      }

      webRtcEndpoint.gatherCandidates();
    } catch (Throwable t) {
      log.error("Play error", t);
      sendError(session, t.getMessage());
    }
  }

  public void sendPlayEnd(WebSocketSession session, MediaPipeline pipeline) {
    try {
      JsonObject response = new JsonObject();
      response.addProperty("id", "playEnd");
      session.sendMessage(new TextMessage(response.toString()));
    } catch (IOException e) {
      log.error("Error sending playEndOfStream message", e);
    }
    // Release pipeline
    pipeline.release();
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
