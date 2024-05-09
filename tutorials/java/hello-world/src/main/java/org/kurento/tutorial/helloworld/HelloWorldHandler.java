/*
 * Copyright 2018 Kurento (https://www.kurento.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kurento.tutorial.helloworld;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

// Kurento client
import org.kurento.client.BaseRtpEndpoint;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;

// Kurento events
import org.kurento.client.ConnectionStateChangedEvent;
import org.kurento.client.ErrorEvent;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.IceComponentStateChangedEvent;
import org.kurento.client.IceGatheringDoneEvent;
import org.kurento.client.MediaFlowInStateChangedEvent;
import org.kurento.client.MediaFlowOutStateChangedEvent;
import org.kurento.client.MediaStateChangedEvent;
import org.kurento.client.MediaTranscodingStateChangedEvent;
import org.kurento.client.NewCandidatePairSelectedEvent;


/**
 * Kurento Java Tutorial - WebSocket message handler.
 */
public class HelloWorldHandler extends TextWebSocketHandler
{
  private static final Logger log = LoggerFactory.getLogger(HelloWorldHandler.class);
  private static final Gson gson = new GsonBuilder().create();

  private final ConcurrentHashMap<String, UserSession> users =
      new ConcurrentHashMap<>();

  @Autowired
  private KurentoClient kurento;

  /**
   * Invoked after WebSocket negotiation has succeeded and the WebSocket connection is
   * opened and ready for use.
   */
  @Override
  public void afterConnectionEstablished(WebSocketSession session)
      throws Exception
  {
    log.info("[HelloWorldHandler::afterConnectionEstablished] New WebSocket connection, sessionId: {}",
        session.getId());
  }

  /**
   * Invoked after the WebSocket connection has been closed by either side, or after a
   * transport error has occurred. Although the session may technically still be open,
   * depending on the underlying implementation, sending messages at this point is
   * discouraged and most likely will not succeed.
   */
  @Override
  public void afterConnectionClosed(final WebSocketSession session,
      CloseStatus status) throws Exception
  {
    if (!status.equalsCode(CloseStatus.NORMAL)) {
      log.warn("[HelloWorldHandler::afterConnectionClosed] status: {}, sessionId: {}",
          status, session.getId());
    }

    stop(session);
  }

  /**
   * Invoked when a new WebSocket message arrives.
   */
  @Override
  protected void handleTextMessage(WebSocketSession session,
      TextMessage message) throws Exception
  {
    final String sessionId = session.getId();
    JsonObject jsonMessage = gson.fromJson(message.getPayload(),
        JsonObject.class);

    log.info("[HelloWorldHandler::handleTextMessage] message: {}, sessionId: {}",
        jsonMessage, sessionId);

    try {
      final String messageId = jsonMessage.get("id").getAsString();
      switch (messageId) {
        case "PROCESS_SDP_OFFER":
          // Start: Create user session and process SDP Offer
          handleProcessSdpOffer(session, jsonMessage);
          break;
        case "ADD_ICE_CANDIDATE":
          handleAddIceCandidate(session, jsonMessage);
          break;
        case "STOP":
          handleStop(session, jsonMessage);
          break;
        case "ERROR":
          handleError(session, jsonMessage);
          break;
        default:
          // Ignore the message
          log.warn("[HelloWorldHandler::handleTextMessage] Skip, invalid message, id: {}",
              messageId);
          break;
      }
    } catch (Throwable ex) {
      log.error("[HelloWorldHandler::handleTextMessage] Exception: {}, sessionId: {}",
          ex, sessionId);
      sendError(session, "[Kurento] Exception: " + ex.getMessage());
    }
  }

  /**
   * Handle an error from the underlying WebSocket message transport.
   */
  @Override
  public void handleTransportError(WebSocketSession session,
      Throwable exception) throws Exception
  {
    log.error("[HelloWorldHandler::handleTransportError] Exception: {}, sessionId: {}",
        exception, session.getId());

    session.close(CloseStatus.SERVER_ERROR);
  }

  private synchronized void sendMessage(final WebSocketSession session,
      String message)
  {
    log.debug("[HelloWorldHandler::sendMessage] {}", message);

    if (!session.isOpen()) {
      log.warn("[HelloWorldHandler::sendMessage] Skip, WebSocket session isn't open");
      return;
    }

    final String sessionId = session.getId();
    if (!users.containsKey(sessionId)) {
      log.warn("[HelloWorldHandler::sendMessage] Skip, unknown user, id: {}",
          sessionId);
      return;
    }

    try {
      session.sendMessage(new TextMessage(message));
    } catch (IOException ex) {
      log.error("[HelloWorldHandler::sendMessage] Exception: {}", ex.getMessage());
    }
  }

  private void sendError(final WebSocketSession session, String errMsg)
  {
    log.error(errMsg);

    if (users.containsKey(session.getId())) {
      JsonObject message = new JsonObject();
      message.addProperty("id", "ERROR");
      message.addProperty("message", errMsg);
      sendMessage(session, message.toString());
    }
  }

  // PROCESS_SDP_OFFER ---------------------------------------------------------

  private void initBaseEventListeners(final WebSocketSession session,
      BaseRtpEndpoint baseRtpEp, final String className)
  {
    log.info("[HelloWorldHandler::initBaseEventListeners] name: {}, class: {}, sessionId: {}",
        baseRtpEp.getName(), className, session.getId());

    // Event: Some error happened
    baseRtpEp.addErrorListener(new EventListener<ErrorEvent>() {
      @Override
      public void onEvent(ErrorEvent ev) {
        log.error("[{}::ErrorEvent] Error code {}: '{}', source: {}, timestamp: {}, tags: {}, description: {}",
            className, ev.getErrorCode(), ev.getType(), ev.getSource().getName(),
            ev.getTimestampMillis(), ev.getTags(), ev.getDescription());

        sendError(session, "[Kurento] " + ev.getDescription());
        stop(session);
      }
    });

    // Event: Media is flowing into this sink
    baseRtpEp.addMediaFlowInStateChangedListener(
        new EventListener<MediaFlowInStateChangedEvent>() {
      @Override
      public void onEvent(MediaFlowInStateChangedEvent ev) {
        log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, state: {}, padName: {}, mediaType: {}",
            className, ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(),
            ev.getTags(), ev.getState(), ev.getPadName(), ev.getMediaType());
      }
    });

    // Event: Media is flowing out of this source
    baseRtpEp.addMediaFlowOutStateChangedListener(
        new EventListener<MediaFlowOutStateChangedEvent>() {
      @Override
      public void onEvent(MediaFlowOutStateChangedEvent ev) {
        log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, state: {}, padName: {}, mediaType: {}",
            className, ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(),
            ev.getTags(), ev.getState(), ev.getPadName(), ev.getMediaType());
      }
    });

    // Event: [TODO write meaning of this event]
    baseRtpEp.addConnectionStateChangedListener(
        new EventListener<ConnectionStateChangedEvent>() {
      @Override
      public void onEvent(ConnectionStateChangedEvent ev) {
        log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, oldState: {}, newState: {}",
            className, ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(),
            ev.getTags(), ev.getOldState(), ev.getNewState());
      }
    });

    // Event: [TODO write meaning of this event]
    baseRtpEp.addMediaStateChangedListener(
        new EventListener<MediaStateChangedEvent>() {
      @Override
      public void onEvent(MediaStateChangedEvent ev) {
        log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, oldState: {}, newState: {}",
            className, ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(),
            ev.getTags(), ev.getOldState(), ev.getNewState());
      }
    });

    // Event: This element will (or will not) perform media transcoding
    baseRtpEp.addMediaTranscodingStateChangedListener(
        new EventListener<MediaTranscodingStateChangedEvent>() {
      @Override
      public void onEvent(MediaTranscodingStateChangedEvent ev) {
        log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, state: {}, binName: {}, mediaType: {}",
            className, ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(),
            ev.getTags(), ev.getState(), ev.getBinName(), ev.getMediaType());
      }
    });
  }

  private void initWebRtcEventListeners(final WebSocketSession session,
      final WebRtcEndpoint webRtcEp)
  {
    log.info("[HelloWorldHandler::initWebRtcEventListeners] name: {}, sessionId: {}",
        webRtcEp.getName(), session.getId());

    // Event: The ICE backend found a local candidate during Trickle ICE
    webRtcEp.addIceCandidateFoundListener(
        new EventListener<IceCandidateFoundEvent>() {
      @Override
      public void onEvent(IceCandidateFoundEvent ev) {
        log.debug("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}, candidate: {}",
            ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(),
            ev.getTags(), JsonUtils.toJson(ev.getCandidate()));

        JsonObject message = new JsonObject();
        message.addProperty("id", "ADD_ICE_CANDIDATE");
        message.add("candidate", JsonUtils.toJsonObject(ev.getCandidate()));
        sendMessage(session, message.toString());
      }
    });

    // Event: The ICE backend changed state
    webRtcEp.addIceComponentStateChangedListener(
        new EventListener<IceComponentStateChangedEvent>() {
      @Override
      public void onEvent(IceComponentStateChangedEvent ev) {
        log.debug("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}, streamId: {}, componentId: {}, state: {}",
            ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(),
            ev.getTags(), ev.getStreamId(), ev.getComponentId(), ev.getState());
      }
    });

    // Event: The ICE backend finished gathering ICE candidates
    webRtcEp.addIceGatheringDoneListener(
        new EventListener<IceGatheringDoneEvent>() {
      @Override
      public void onEvent(IceGatheringDoneEvent ev) {
        log.info("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}",
            ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(),
            ev.getTags());
      }
    });

    // Event: The ICE backend selected a new pair of ICE candidates for use
    webRtcEp.addNewCandidatePairSelectedListener(
        new EventListener<NewCandidatePairSelectedEvent>() {
      @Override
      public void onEvent(NewCandidatePairSelectedEvent ev) {
        log.info("[WebRtcEndpoint::{}] name: {}, timestamp: {}, tags: {}, streamId: {}, local: {}, remote: {}",
            ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(),
            ev.getTags(), ev.getCandidatePair().getStreamId(),
            ev.getCandidatePair().getLocalCandidate(),
            ev.getCandidatePair().getRemoteCandidate());
      }
    });
  }

  private void initWebRtcEndpoint(final WebSocketSession session,
      final WebRtcEndpoint webRtcEp, String sdpOffer)
  {
    initBaseEventListeners(session, webRtcEp, "WebRtcEndpoint");
    initWebRtcEventListeners(session, webRtcEp);

    final String sessionId = session.getId();
    final String name = "user" + sessionId + "_webrtcendpoint";
    webRtcEp.setName(name);

    /*
    OPTIONAL: Force usage of an Application-specific STUN server.
    Usually this is configured globally in KMS WebRTC settings file:
    /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini

    But it can also be configured per-application, as shown:

    log.info("[HelloWorldHandler::initWebRtcEndpoint] Using STUN server: 193.147.51.12:3478");
    webRtcEp.setStunServerAddress("193.147.51.12");
    webRtcEp.setStunServerPort(3478);
    */

    // Continue the SDP Negotiation: Generate an SDP Answer
    final String sdpAnswer = webRtcEp.processOffer(sdpOffer);

    log.info("[HelloWorldHandler::initWebRtcEndpoint] name: {}, SDP Offer from browser to KMS:\n{}",
        name, sdpOffer);
    log.info("[HelloWorldHandler::initWebRtcEndpoint] name: {}, SDP Answer from KMS to browser:\n{}",
        name, sdpAnswer);

    JsonObject message = new JsonObject();
    message.addProperty("id", "PROCESS_SDP_ANSWER");
    message.addProperty("sdpAnswer", sdpAnswer);
    sendMessage(session, message.toString());
  }

  private void startWebRtcEndpoint(WebRtcEndpoint webRtcEp)
  {
    // Calling gatherCandidates() is when the Endpoint actually starts working.
    // In this tutorial, this is emphasized for demonstration purposes by
    // launching the ICE candidate gathering in its own method.
    webRtcEp.gatherCandidates();
  }

  private void handleProcessSdpOffer(final WebSocketSession session,
      JsonObject jsonMessage)
  {
    // ---- Session handling

    final String sessionId = session.getId();

    log.info("[HelloWorldHandler::handleStart] User count: {}", users.size());
    log.info("[HelloWorldHandler::handleStart] New user, id: {}", sessionId);

    final UserSession user = new UserSession();
    users.put(sessionId, user);


    // ---- Media pipeline

    log.info("[HelloWorldHandler::handleStart] Create Media Pipeline");

    final MediaPipeline pipeline = kurento.createMediaPipeline();
    user.setMediaPipeline(pipeline);

    final WebRtcEndpoint webRtcEp =
        new WebRtcEndpoint.Builder(pipeline).build();
    user.setWebRtcEndpoint(webRtcEp);
    webRtcEp.connect(webRtcEp);


    // ---- Endpoint configuration

    String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
    initWebRtcEndpoint(session, webRtcEp, sdpOffer);

    log.info("[HelloWorldHandler::handleStart] New WebRtcEndpoint: {}",
        webRtcEp.getName());


    // ---- Endpoint startup

    startWebRtcEndpoint(webRtcEp);


    // ---- Debug
    // final String pipelineDot = pipeline.getGstreamerDot();
    // try (PrintWriter out = new PrintWriter("pipeline.dot")) {
    //   out.println(pipelineDot);
    // } catch (IOException ex) {
    //   log.error("[HelloWorldHandler::start] Exception: {}", ex.getMessage());
    // }
  }

  // ADD_ICE_CANDIDATE ---------------------------------------------------------

  private void handleAddIceCandidate(final WebSocketSession session,
      JsonObject jsonMessage)
  {
    final String sessionId = session.getId();
    if (!users.containsKey(sessionId)) {
      log.warn("[HelloWorldHandler::handleAddIceCandidate] Skip, unknown user, id: {}",
          sessionId);
      return;
    }

    final UserSession user = users.get(sessionId);
    final JsonObject jsonCandidate =
        jsonMessage.get("candidate").getAsJsonObject();
    final IceCandidate candidate =
        new IceCandidate(jsonCandidate.get("candidate").getAsString(),
        jsonCandidate.get("sdpMid").getAsString(),
        jsonCandidate.get("sdpMLineIndex").getAsInt());

    WebRtcEndpoint webRtcEp = user.getWebRtcEndpoint();
    webRtcEp.addIceCandidate(candidate);
  }

  // STOP ----------------------------------------------------------------------

  private void stop(final WebSocketSession session)
  {
    // Remove the user session and release all resources
    final UserSession user = users.remove(session.getId());
    if (user != null) {
      MediaPipeline mediaPipeline = user.getMediaPipeline();
      if (mediaPipeline != null) {
        log.info("[HelloWorldHandler::stop] Release the Media Pipeline");
        mediaPipeline.release();
      }
    }
  }

  private void handleStop(final WebSocketSession session,
      JsonObject jsonMessage)
  {
    stop(session);
  }

  // ERROR ---------------------------------------------------------------------

  private void handleError(final WebSocketSession session,
      JsonObject jsonMessage)
  {
    final String errMsg = jsonMessage.get("message").getAsString();
    log.error("Browser error: " + errMsg);

    log.info("Assume that the other side stops after an error...");
    stop(session);
  }

  // ---------------------------------------------------------------------------
}
