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

package org.kurento.tutorial.rtpreceiver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.kurento.client.RtpEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;

// Kurento crypto
import org.kurento.client.CryptoSuite;
import org.kurento.client.SDES;

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
import org.kurento.client.OnKeySoftLimitEvent;


/**
 * Kurento Java Tutorial - Handler class.
 */
public class Handler extends TextWebSocketHandler
{
  private final Logger log = LoggerFactory.getLogger(Handler.class);
  private final Gson gson = new GsonBuilder().create();

  private final ConcurrentHashMap<String, UserSession> users =
      new ConcurrentHashMap<>();

  @Autowired
  private KurentoClient kurento;

  @Override
  public void afterConnectionClosed(final WebSocketSession session,
      CloseStatus status) throws Exception
  {
    log.debug("[Handler::afterConnectionClosed] status: {}, sessionId: {}",
        status, session.getId());

    stop(session);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session,
      TextMessage message) throws Exception
  {
    JsonObject jsonMessage = gson.fromJson(message.getPayload(),
        JsonObject.class);
    String sessionId = session.getId();

    log.debug("[Handler::handleTextMessage] {}, sessionId: {}",
        jsonMessage, sessionId);

    try {
      String messageId = jsonMessage.get("id").getAsString();
      switch (messageId) {
        case "PROCESS_SDP_OFFER":
          handleProcessSdpOffer(session, jsonMessage);
          break;
        case "ADD_ICE_CANDIDATE":
          handleAddIceCandidate(session, jsonMessage);
          break;
        case "STOP":
          handleStop(session, jsonMessage);
          break;
        default:
          sendError(session, "Invalid message, id: " + messageId);
          break;
      }
    } catch (Throwable ex) {
      log.error("[Handler::handleTextMessage] Exception: {}, sessionId: {}",
          ex, sessionId);
      sendError(session, "Exception: " + ex.getMessage());
    }
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable ex)
      throws Exception
  {
    log.error("[Handler::handleTransportError] Exception: {}, sessionId: {}",
        ex, session.getId());
  }

  // PROCESS_SDP_OFFER ---------------------------------------------------------

  private void addBaseEventListeners(final WebSocketSession session,
      BaseRtpEndpoint baseRtpEp, final String className)
  {
    log.info("[Handler::addBaseEventListeners] name: {}, class: {}, sessionId: {}",
        baseRtpEp.getName(), className, session.getId());

    // Event: Some error happened
    baseRtpEp.addErrorListener(new EventListener<ErrorEvent>() {
      @Override
      public void onEvent(ErrorEvent ev) {
        log.error("[{}::{}] source: {}, timestamp: {}, tags: {}, description: {}, errorCode: {}",
            className, ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(),
            ev.getTags(), ev.getDescription(), ev.getErrorCode());
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

  private void addWebRtcEventListeners(final WebSocketSession session,
      final WebRtcEndpoint webRtcEp)
  {
    log.info("[Handler::addWebRtcEventListeners] name: {}, sessionId: {}",
        webRtcEp.getName(), session.getId());

    // Event: The ICE backend found a local candidate during Trickle ICE
    webRtcEp.addIceCandidateFoundListener(
        new EventListener<IceCandidateFoundEvent>() {
      @Override
      public void onEvent(IceCandidateFoundEvent ev) {
        log.debug("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}, candidate: {}",
            ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(),
            ev.getTags(), JsonUtils.toJsonObject(ev.getCandidate()));

        JsonObject message = new JsonObject();
        message.addProperty("id", "ADD_ICE_CANDIDATE");
        message.addProperty("webRtcEpId", webRtcEp.getId());
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
        log.debug("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}",
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
    addBaseEventListeners(session, webRtcEp, "WebRtcEndpoint");
    addWebRtcEventListeners(session, webRtcEp);

    /*
    OPTIONAL: Force usage of an Application-specific STUN server.
    Usually this is configured globally in KMS WebRTC settings file:
    /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini

    But it can also be configured per-application, as shown:

    log.info("[Handler::initWebRtcEndpoint] Using STUN server: 193.147.51.12:3478");
    webRtcEp.setStunServerAddress("193.147.51.12");
    webRtcEp.setStunServerPort(3478);
    */

    // Process the SDP Offer to generate an SDP Answer
    String sdpAnswer = webRtcEp.processOffer(sdpOffer);

    log.info("[Handler::initWebRtcEndpoint] name: {}, SDP Offer from browser to KMS:\n{}",
        webRtcEp.getName(), sdpOffer);
    log.info("[Handler::initWebRtcEndpoint] name: {}, SDP Answer from KMS to browser:\n{}",
        webRtcEp.getName(), sdpAnswer);

    JsonObject message = new JsonObject();
    message.addProperty("id", "PROCESS_SDP_ANSWER");
    message.addProperty("sdpAnswer", sdpAnswer);
    sendMessage(session, message.toString());
  }

  private void startWebRtcEndpoint(WebRtcEndpoint webRtcEp)
  {
    // Calling gatherCandidates() is when the Endpoint actually starts working.
    // In this tutorial, this is emphasized for demonstration purposes by
    // leaving the ICE candidate gathering in its own method.
    webRtcEp.gatherCandidates();
  }

  private RtpEndpoint makeRtpEndpoint(MediaPipeline pipeline, Boolean useSrtp)
  {
    if (!useSrtp) {
      return new RtpEndpoint.Builder(pipeline).build();
    }

    // ---- SRTP configuration BEGIN ----
    // This is used by KMS to encrypt its SRTP/SRTCP packets.
    // Encryption key used by receiver (ASCII): "4321ZYXWVUTSRQPONMLKJIHGFEDCBA"
    // In Base64: "NDMyMVpZWFdWVVRTUlFQT05NTEtKSUhHRkVEQ0JB"
    CryptoSuite srtpCrypto = CryptoSuite.AES_128_CM_HMAC_SHA1_80;
    // CryptoSuite crypto = CryptoSuite.AES_256_CM_HMAC_SHA1_80;

    // You can provide the SRTP Master Key in either plain text or Base64.
    // The second form allows providing binary, non-ASCII keys.
    String srtpMasterKeyAscii = "4321ZYXWVUTSRQPONMLKJIHGFEDCBA";
    // String srtpMasterKeyBase64 = "NDMyMVpZWFdWVVRTUlFQT05NTEtKSUhHRkVEQ0JB";
    // ---- SRTP configuration END ----

    SDES sdes = new SDES();
    sdes.setCrypto(srtpCrypto);
    sdes.setKey(srtpMasterKeyAscii);
    // sdes.setKeyBase64(srtpMasterKeyBase64);

    return new RtpEndpoint.Builder(pipeline).withCrypto(sdes).build();
  }

  /*
  RtpEndpoint configuration.
  Controls the SDP Offer/Answer negotiation between a 3rd-party RTP sender
  and KMS. A fake SDP Offer simulates the features that our RTP sender
  will have; then, the SDP Answer is parsed to extract the RTP and RTCP ports
  that KMS will use to listen for packets.
  */
  private void startRtpEndpoint(final WebSocketSession session,
      RtpEndpoint rtpEp, Boolean useComedia, Boolean useSrtp)
  {
    log.info("[Handler::startRtpEndpoint] Configure RtpEndpoint, port discovery: {}, SRTP: {}",
        useComedia, useSrtp);

    addBaseEventListeners(session, rtpEp, "RtpEndpoint");

    // Event: The SRTP key is about to expire
    rtpEp.addOnKeySoftLimitListener(
        new EventListener<OnKeySoftLimitEvent>() {
      @Override
      public void onEvent(OnKeySoftLimitEvent ev) {
        log.info("[RtpEndpoint::{}] source: {}, timestamp: {}, tags: {}, mediaType: {}",
            ev.getType(), ev.getSource(), ev.getTimestampMillis(), ev.getTags(),
            ev.getMediaType());
      }
    });

    // ---- RTP configuration BEGIN ----
    // Set the appropriate values for your setup
    String senderIp = "127.0.0.1";
    int senderRtpPortA = 5006;
    int senderRtpPortV = 5004;
    int senderSsrcA = 445566;
    int senderSsrcV = 112233;
    String senderCname = "user@example.com";
    String senderCodecV = "H264";
    // String senderCodecV = "VP8";
    // ---- RTP configuration END ----

    /*
    OPTIONAL: Set maximum bandwidth on reception.
    This can be useful if there is some limitation on the incoming bandwidth
    that the receiver is able to process.
    */
    // log.info("[Handler::startRtpEndpoint] Limit output bandwidth: 1024 kbps");
    // rtpEp.setMaxVideoRecvBandwidth(1024); // In kbps (1000 bps)

    String sdpComediaAttr = "";
    if (useComedia) {
      // Use Discard port (9)
      senderRtpPortA = 9;
      senderRtpPortV = 9;

      // Inspired by RFC 4145 Draft 05 ("COMEDIA")
      sdpComediaAttr = "a=direction:active\r\n";
    }

    Boolean useAudio = true;
    String senderProtocol = "RTP/AVPF";
    String sdpCryptoAttr = "";
    if (useSrtp) {
      // Use SRTP protocol
      useAudio = false;  // This demo uses audio only for non-SRTP streams
      senderProtocol = "RTP/SAVPF";

      // This is used by KMS to decrypt the sender's SRTP/SRTCP
      // Encryption key used by sender (ASCII): "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234"
      // In Base64: "QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVoxMjM0"
      sdpCryptoAttr = "a=crypto:2 AES_CM_128_HMAC_SHA1_80 inline:QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVoxMjM0|2^31|1:1\r\n";
    }

/*
# SDP quick reference
SDP structure is composed of levels: session > media > source.
Each level can contain one or more of the next ones.
Typically, one session contains several medias, and each media contains one source.

---- Session-level information ----
v=
o=
s=
c=
t=
---- Media-level attributes ----
m=
a=
---- Source-level attributes ----
a=ssrc

Some default values are defined by different RFCs:
- RFC 3264 defines recommended values for "s=", "t=", "a=sendonly".
- RFC 5576 defines source-level attribute "a=ssrc".
*/

    String rtpSdpOffer =
        "v=0\r\n"
        + "o=- 0 0 IN IP4 " + senderIp + "\r\n"
        + "s=Kurento Tutorial - RTP Receiver\r\n"
        + "c=IN IP4 " + senderIp + "\r\n"
        + "t=0 0\r\n";

    if (useAudio) {
      rtpSdpOffer +=
          "m=audio " + senderRtpPortA + " RTP/AVPF 96\r\n"
          + "a=rtpmap:96 opus/48000/2\r\n"
          + "a=sendonly\r\n"
          + sdpComediaAttr
          + "a=ssrc:" + senderSsrcA + " cname:" + senderCname + "\r\n";
    }

    rtpSdpOffer +=
        "m=video " + senderRtpPortV + " " + senderProtocol + " 103\r\n"
        + sdpCryptoAttr
        + "a=rtpmap:103 " + senderCodecV + "/90000\r\n"
        + "a=rtcp-fb:103 goog-remb\r\n"
        + "a=sendonly\r\n"
        + sdpComediaAttr
        + "a=ssrc:" + senderSsrcV + " cname:" + senderCname + "\r\n"
        + "";

    // Send the SDP Offer to KMS, and get its negotiated SDP Answer
    String rtpSdpAnswer = rtpEp.processOffer(rtpSdpOffer);

    log.info("[Handler::startRtpEndpoint] Fake SDP Offer from App to KMS:\n{}",
        rtpSdpOffer);
    log.info("[Handler::startRtpEndpoint] SDP Answer from KMS to App:\n{}",
        rtpSdpAnswer);

    // Parse SDP Answer
    // NOTE: No error checking; this code assumes that the SDP Answer from KMS
    // is always well formed.
    Pattern p; Matcher m;

    int kmsRtpPortA = 0;
    int senderRtcpPortA = 0;
    if (useAudio) {
      p = Pattern.compile("m=audio (\\d+) RTP");
      m = p.matcher(rtpSdpAnswer);
      m.find();
      kmsRtpPortA = Integer.parseInt(m.group(1));
      senderRtcpPortA = senderRtpPortA + 1;
    }

    p = Pattern.compile("m=video (\\d+) RTP");
    m = p.matcher(rtpSdpAnswer);
    m.find();
    int kmsRtpPortV = Integer.parseInt(m.group(1));
    int senderRtcpPortV = senderRtpPortV + 1;

    p = Pattern.compile("a=ssrc:(\\d+)");
    m = p.matcher(rtpSdpAnswer);
    m.find();
    String kmsSsrcV = m.group(1);

    p = Pattern.compile("c=IN IP4 (([0-9]{1,3}\\.){3}[0-9]{1,3})");
    m = p.matcher(rtpSdpAnswer);
    m.find();
    String kmsIp = m.group(1);

    // Check if KMS accepted the use of "direction" attribute
    useComedia = rtpSdpAnswer.contains("a=direction:passive");

    String msgConnInfo = "SDP negotiation finished\n";
    if (useAudio) {
      msgConnInfo += String.format(
          "* KMS listens for Audio RTP at port: %d\n", kmsRtpPortA);
    }
    msgConnInfo += String.format(
        "* KMS listens for Video RTP at port: %d\n", kmsRtpPortV);
    if (useSrtp) {
      msgConnInfo += String.format(
          "* KMS uses Video SSRC: %s\n", kmsSsrcV);
    }
    if (useAudio) {
      msgConnInfo += String.format(
          "* KMS expects Audio SSRC from sender: %d\n", senderSsrcA);
    }
    msgConnInfo += String.format(
        "* KMS expects Video SSRC from sender: %d\n", senderSsrcV);
    msgConnInfo += String.format("* KMS local IP address: %s\n", kmsIp);
    if (useComedia) {
      msgConnInfo += "* KMS will discover remote IP and port to send RTCP\n";
    } else {
      if (useAudio) {
        msgConnInfo += String.format(
            "* KMS sends Audio RTCP to: %s:%d\n", senderIp, senderRtcpPortA);
      }
      msgConnInfo += String.format(
          "* KMS sends Video RTCP to: %s:%d\n", senderIp, senderRtcpPortV);
    }

    log.info("[Handler::startRtpEndpoint] " + msgConnInfo);

    // Send info to UI
    {
      JsonObject message = new JsonObject();
      message.addProperty("id", "SHOW_CONN_INFO");
      message.addProperty("text", msgConnInfo);
      sendMessage(session, message.toString());
    }
    {
      JsonObject message = new JsonObject();
      message.addProperty("id", "SHOW_SDP_ANSWER");
      message.addProperty("text", rtpSdpAnswer);
      sendMessage(session, message.toString());
    }
  }

  private void handleProcessSdpOffer(final WebSocketSession session,
      JsonObject jsonMessage)
  {
    // ---- Session handling

    String sessionId = session.getId();

    log.info("[Handler::handleStart] User count: {}", users.size());
    log.info("[Handler::handleStart] New user: {}", sessionId);

    final UserSession user = new UserSession();
    users.put(session.getId(), user);


    // ---- Media pipeline

    log.info("[Handler::handleStart] Create Media Pipeline");
    final MediaPipeline pipeline = kurento.createMediaPipeline();
    user.setMediaPipeline(pipeline);

    final WebRtcEndpoint webRtcEp =
        new WebRtcEndpoint.Builder(pipeline).build();
    user.setWebRtcEndpoint(webRtcEp);

    Boolean useSrtp = jsonMessage.get("useSrtp").getAsBoolean();
    final RtpEndpoint rtpEp = makeRtpEndpoint(pipeline, useSrtp);
    user.setRtpEndpoint(rtpEp);


    // ---- Endpoint configuration

    rtpEp.connect(webRtcEp);

    String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
    initWebRtcEndpoint(session, webRtcEp, sdpOffer);
    startWebRtcEndpoint(webRtcEp);

    Boolean useComedia = jsonMessage.get("useComedia").getAsBoolean();
    startRtpEndpoint(session, rtpEp, useComedia, useSrtp);


    // ---- Debug
    // String pipelineDot = pipeline.getGstreamerDot();
    // try (PrintWriter out = new PrintWriter("pipeline.dot")) {
    //   out.println(pipelineDot);
    // } catch (IOException ex) {
    //   log.error("[Handler::start] Exception: {}", ex.getMessage());
    // }
  }

  // ADD_ICE_CANDIDATE ---------------------------------------------------------

  private void handleAddIceCandidate(final WebSocketSession session,
      JsonObject jsonMessage)
  {
    String sessionId = session.getId();
    UserSession user = users.get(sessionId);

    if (user != null) {
      JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();
      IceCandidate candidate =
          new IceCandidate(jsonCandidate.get("candidate").getAsString(),
          jsonCandidate.get("sdpMid").getAsString(),
          jsonCandidate.get("sdpMLineIndex").getAsInt());

      WebRtcEndpoint webRtcEp = user.getWebRtcEndpoint();
      webRtcEp.addIceCandidate(candidate);
    }
  }

  // STOP ----------------------------------------------------------------------

  public void sendPlayEnd(final WebSocketSession session)
  {
    if (users.containsKey(session.getId())) {
      JsonObject message = new JsonObject();
      message.addProperty("id", "END_PLAYBACK");
      sendMessage(session, message.toString());
    }
  }

  private void stop(final WebSocketSession session)
  {
    log.info("[Handler::stop]");

    // Update the UI
    sendPlayEnd(session);

    // Remove the user session and release all resources
    String sessionId = session.getId();
    UserSession user = users.remove(sessionId);
    if (user != null) {
      MediaPipeline mediaPipeline = user.getMediaPipeline();
      if (mediaPipeline != null) {
        log.info("[Handler::stop] Release the Media Pipeline");
        mediaPipeline.release();
      }
    }
  }

  private void handleStop(final WebSocketSession session,
      JsonObject jsonMessage)
  {
    stop(session);
  }

  // ---------------------------------------------------------------------------

  private void sendError(final WebSocketSession session, String errMsg)
  {
    if (users.containsKey(session.getId())) {
      JsonObject message = new JsonObject();
      message.addProperty("id", "ERROR");
      message.addProperty("message", errMsg);
      sendMessage(session, message.toString());
    }
  }

  private synchronized void sendMessage(final WebSocketSession session,
      String message)
  {
    if (!session.isOpen()) {
      log.error("[Handler::sendMessage] WebSocket session is closed");
      return;
    }

    try {
      session.sendMessage(new TextMessage(message));
    } catch (IOException ex) {
      log.error("[Handler::sendMessage] Exception: {}", ex.getMessage());
    }
  }
}
