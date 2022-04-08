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

package org.kurento.test.browser;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.MediaStateChangedEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.test.base.KurentoTest;
import org.kurento.test.latency.VideoTagType;
import org.kurento.test.monitor.PeerConnectionStats;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Specific client for tests within kurento-test project. This logic is linked to client page logic
 * (e.g. webrtc.html).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.1.0
 */
public class WebRtcTestPage extends WebPage {

  public interface WebRtcConfigurer {
    public void addIceCandidate(IceCandidate candidate);

    public String processOffer(String sdpOffer);
  }

  protected final String FAKE_IPV4 = "10.2.3.4";
  protected final String FAKE_IPV6 = "2000:2001:2002:2003:2004:2005:2006";

  protected static final String LOCAL_VIDEO = "local";
  protected static final String REMOTE_VIDEO = "video";

  public WebRtcTestPage() {
  }

  @Override
  public void setBrowser(Browser browserClient) {
    super.setBrowser(browserClient);

    // By default all tests are going to track color in both video tags
    checkColor(LOCAL_VIDEO, REMOTE_VIDEO);

    VideoTagType.setLocalId(LOCAL_VIDEO);
    VideoTagType.setRemoteId(REMOTE_VIDEO);
  }

  /*
   * setColorCoordinates
   */
  @Override
  public void setColorCoordinates(int x, int y) {
    browser.getWebDriver().findElement(By.id("x")).clear();
    browser.getWebDriver().findElement(By.id("y")).clear();
    browser.getWebDriver().findElement(By.id("x")).sendKeys(String.valueOf(x));
    browser.getWebDriver().findElement(By.id("y")).sendKeys(String.valueOf(y));
    super.setColorCoordinates(x, y);
  }

  /*
   * similarColor
   */
  public boolean similarColor(Color expectedColor) {
    return similarColor(REMOTE_VIDEO, expectedColor);

  }

  /*
   * similarColorAt
   */
  public boolean similarColorAt(Color expectedColor, int x, int y) {
    return similarColorAt(REMOTE_VIDEO, expectedColor, x, y);
  }

  /*
   * subscribeEvents
   */
  public void subscribeEvents(String eventType) {
    subscribeEventsToVideoTag(REMOTE_VIDEO, eventType);
  }

  /*
   * subscribeLocalEvents
   */
  public void subscribeLocalEvents(String eventType) {
    subscribeEventsToVideoTag(LOCAL_VIDEO, eventType);
  }

  /*
   * start
   */
  public void start(String videoUrl) {
    browser.executeScript("play('" + videoUrl + "', false);");
  }

  /*
   * stop
   */
  public void stopPlay() {
    browser.executeScript("terminate();");
  }

  /*
   * getCurrentTime
   */
  public double getCurrentTime() {
    log.debug("getCurrentTime() called");
    double currentTime = Double.parseDouble(
        browser.getWebDriver().findElement(By.id("currentTime")).getAttribute("value"));
    log.debug("getCurrentTime() result: {}", currentTime);
    return currentTime;
  }

  /*
   * readConsole
   */
  public String readConsole() {
    return browser.getWebDriver().findElement(By.id("console")).getText();
  }

  public void sendDataByDataChannel(String message) {
    browser.executeScript("sendDataByChannel('" + message + "')");
  }

  public boolean checkAudioDetection() {
    boolean checkAudio = (boolean) browser.executeScript("return checkAudioDetection()");
    log.debug("Checking Audio: {}", checkAudio);
    return checkAudio;
  }

  public void activateAudioDetection() {
    browser.executeScript("activateAudioDetection()");
  }

  public void stopAudioDetection() {
    browser.executeScript("stopAudioDetection()");
  }

  public void initAudioDetection() {
    browser.executeScript("initAudioDetection()");
  }

  public int getPeerConnAudioPacketsRecv(PeerConnectionStats stats) {
    String val = (String) stats.getStats().get("audio_peerconnection_inbound_packetsReceived");
    return Integer.parseInt(val);
  }

  public long getPeerConnAudioInboundTimestamp(PeerConnectionStats stats) {
    Long val = (Long) stats.getStats().get("audio_peerconnection_inbound_timestamp");
    return val;
  }

  /*
   * compare
   */
  public boolean compare(double i, double j) {
    return Math.abs(j - i) <= browser.getThresholdTime();
  }

  protected void addIceCandidate(JsonObject candidate) {
    browser.executeScript("addIceCandidate('" + candidate + "');");
  }

  /*
   * Decide if one candidate has to be added or not according with two parameters
   */
  protected Boolean filterCandidate(String candidate, WebRtcIpvMode webRtcIpvMode,
      WebRtcCandidateType webRtcCandidateType) {

    Boolean filtered = true;
    Boolean hasCandidateIpv6 = false;
    if (candidate.split("candidate:")[1].contains(":")) {
      hasCandidateIpv6 = true;
    }

    switch (webRtcIpvMode) {
      case IPV4:
        if (!hasCandidateIpv6) {
          filtered = false;
        }
        break;
      case IPV6:
        if (hasCandidateIpv6) {
          filtered = false;
        }
        break;
      case BOTH:
      default:
        filtered = false;
        break;
    }
    return filtered;
  }

  protected String manglingCandidate(String candidate, WebRtcIpvMode webRtcIpvMode,
      WebRtcCandidateType webRtcCandidateType) {

    String internalAddress;
    String publicAddress;
    String internalAddresses[];
    String publicAddresses[];
    String newInternalAddress = "";
    String newPublicAddress = "";

    String candidateType = candidate.split("typ")[1].split(" ")[1];

    if (WebRtcCandidateType.HOST.toString().equals(candidateType)) {
      internalAddress = candidate.split(" ")[4];
      switch (webRtcIpvMode) {
        case IPV4:
          if (!webRtcCandidateType.toString().equals(candidateType)) {
            internalAddresses = internalAddress.split("\\.");
            for (int i = 0; i < internalAddresses.length - 1; i++) {
              newInternalAddress = newInternalAddress.concat(internalAddresses[i] + ".");
            }
            newInternalAddress = newInternalAddress.concat("254");
          } else {
            newInternalAddress = internalAddress;
          }
          return candidate.replace(internalAddress, newInternalAddress);
        case IPV6:
          if (!webRtcCandidateType.toString().equals(candidateType)) {
            internalAddresses = internalAddress.split(":");
            for (int i = 0; i < internalAddresses.length - 1; i++) {
              newInternalAddress = newInternalAddress.concat(internalAddresses[i] + ":");
            }
            newInternalAddress = newInternalAddress.concat("2000");
          } else {
            newInternalAddress = internalAddress;
          }
          return candidate.replace(internalAddress, newInternalAddress);
        default:
          break;
      }

    } else if (WebRtcCandidateType.SRFLX.toString().equals(candidateType)
        || WebRtcCandidateType.RELAY.toString().equals(candidateType)) {
      publicAddress = candidate.split(" ")[4];
      internalAddress = candidate.split(" ")[9];
      switch (webRtcIpvMode) {
        case IPV4:
          internalAddresses = internalAddress.split("\\.");

          for (int i = 0; i < internalAddresses.length - 1; i++) {
            newInternalAddress = newInternalAddress.concat(internalAddresses[i] + ".");
          }
          newInternalAddress = newInternalAddress.concat("254");

          if (!webRtcCandidateType.toString().equals(candidateType)) {
            publicAddresses = publicAddress.split("\\.");
            for (int i = 0; i < publicAddresses.length - 1; i++) {
              newPublicAddress = newPublicAddress.concat(publicAddresses[i] + ".");
            }
            newPublicAddress = newPublicAddress.concat("254");
          } else {
            newPublicAddress = publicAddress;
          }
          return candidate.replace(internalAddress, newInternalAddress).replace(publicAddress,
              newPublicAddress);
        case IPV6:
          internalAddresses = internalAddress.split(":");
          for (int i = 0; i < internalAddresses.length - 1; i++) {
            newInternalAddress = newInternalAddress.concat(internalAddresses[i] + ":");
          }
          newInternalAddress = newInternalAddress.concat("2000");

          if (!webRtcCandidateType.toString().equals(candidateType)) {
            publicAddresses = publicAddress.split(":");
            for (int i = 0; i < publicAddresses.length - 1; i++) {
              newPublicAddress = newPublicAddress.concat(publicAddresses[i] + ":");
            }
            newPublicAddress = newPublicAddress.concat("2000");
          } else {
            newPublicAddress = publicAddress;
          }
          return candidate.replace(internalAddress, newInternalAddress).replace(publicAddress,
              newPublicAddress);
        default:
          break;
      }
    }

    return candidate;
  }

  /*
   * initWebRtc with IPVMode
   */
  public void initWebRtc(final WebRtcEndpoint webRtcEndpoint, final WebRtcChannel channel,
      final WebRtcMode mode, final WebRtcIpvMode webRtcIpvMode,
      final WebRtcCandidateType webRtcCandidateType, boolean useDataChannels)
      throws InterruptedException {

    webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

      @Override
      public void onEvent(IceCandidateFoundEvent event) {
        JsonObject candidate = JsonUtils.toJsonObject(event.getCandidate());

        if (!filterCandidate(candidate.get("candidate").getAsString(), webRtcIpvMode,
            webRtcCandidateType)) {
          log.debug("OnIceCandadite -> Adding candidate: {} IpvMode: {} CandidateType: {}",
              candidate.get("candidate").getAsString(), webRtcIpvMode, webRtcCandidateType);
          addIceCandidate(candidate);
        }
      }
    });

    webRtcEndpoint.addMediaStateChangedListener(new EventListener<MediaStateChangedEvent>() {
      @Override
      public void onEvent(MediaStateChangedEvent event) {
        log.debug("MediaStateChangedEvent from {} to {} on {} at {}", event.getOldState(),
            event.getNewState(), webRtcEndpoint.getId(), event.getTimestampMillis());
      }
    });

    WebRtcConfigurer webRtcConfigurer = new WebRtcConfigurer() {
      @Override
      public void addIceCandidate(IceCandidate candidate) {

        if (!filterCandidate(candidate.getCandidate(), webRtcIpvMode, webRtcCandidateType)) {
          log.debug("webRtcConfigurer -> Adding candidate: {} IpvMode: {} CandidateType: {}",
              candidate.getCandidate(), webRtcIpvMode, webRtcCandidateType);
          webRtcEndpoint.addIceCandidate(candidate);
        }
      }

      @Override
      public String processOffer(String sdpOffer) {
        String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);
        webRtcEndpoint.gatherCandidates();
        return sdpAnswer;
      }
    };

    initWebRtc(webRtcConfigurer, channel, mode, webRtcCandidateType, useDataChannels);
  }

  /**
   *
   * initWebRtc with IPVMode and without useDataChannels
   */
  public void initWebRtc(final WebRtcEndpoint webRtcEndpoint, final WebRtcChannel channel,
      final WebRtcMode mode, final WebRtcIpvMode webRtcIpvMode,
      final WebRtcCandidateType webRtcCandidateType) throws InterruptedException {
    initWebRtc(webRtcEndpoint, channel, mode, webRtcIpvMode, webRtcCandidateType, false);
  }

  /*
   * initWebRtc with IPVMode
   */
  public void initWebRtc(final WebRtcEndpoint webRtcEndpoint, final WebRtcChannel channel,
      final WebRtcMode mode, final WebRtcIpvMode webRtcIpvMode) throws InterruptedException {
    initWebRtc(webRtcEndpoint, channel, mode, webRtcIpvMode, WebRtcCandidateType.ALL, false);
  }

  /*
   * initWebRtc with useDataChannels
   */
  public void initWebRtc(final WebRtcEndpoint webRtcEndpoint, final WebRtcChannel channel,
      final WebRtcMode mode, final Boolean useDataChannels) throws InterruptedException {
    initWebRtc(webRtcEndpoint, channel, mode, WebRtcIpvMode.BOTH, WebRtcCandidateType.ALL,
        useDataChannels);
  }

  /*
   * initWebRtc without IPVMode
   */
  public void initWebRtc(final WebRtcEndpoint webRtcEndpoint, final WebRtcChannel channel,
      final WebRtcMode mode) throws InterruptedException {
    initWebRtc(webRtcEndpoint, channel, mode, WebRtcIpvMode.BOTH, WebRtcCandidateType.ALL, false);
  }

  @SuppressWarnings({ "unchecked", "deprecation" })
  protected void initWebRtc(final WebRtcConfigurer webRtcConfigurer, final WebRtcChannel channel,
      final WebRtcMode mode, final WebRtcCandidateType candidateType, boolean useDataChannels)
      throws InterruptedException {
    // ICE candidates
    Thread t1 = new Thread() {
      @Override
      public void run() {
        JsonParser parser = new JsonParser();
        int numCandidate = 0;
        while (true) {
          try {
            ArrayList<Object> iceCandidates =
                (ArrayList<Object>) browser.executeScript("return iceCandidates;");

            for (int i = numCandidate; i < iceCandidates.size(); i++) {
              JsonObject jsonCandidate = (JsonObject) parser.parse(iceCandidates.get(i).toString());
              IceCandidate candidate =
                  new IceCandidate(jsonCandidate.get("candidate").getAsString(),
                      jsonCandidate.get("sdpMid").getAsString(),
                      jsonCandidate.get("sdpMLineIndex").getAsInt());
              // log.debug("Adding candidate {}: {}", i, jsonCandidate);
              webRtcConfigurer.addIceCandidate(candidate);
              numCandidate++;
            }

            // Poll 300 ms
            Thread.sleep(300);

          } catch (Throwable e) {
            log.debug("Exiting gather candidates thread");
            break;
          }
        }
      }
    };
    t1.start();

    // Append WebRTC mode (send/receive and audio/video) to identify test
    addTestName(KurentoTest.getTestClassName() + "." + KurentoTest.getTestMethodName());
    appendStringToTitle(mode.toString());
    appendStringToTitle(channel.toString());

    // Setting custom audio stream (if necessary)
    String audio = browser.getAudio();
    if (audio != null) {
      browser.executeScript("setCustomAudio('" + audio + "');");
    }

    // Create peerConnection for using dataChannels (if necessary)
    if (useDataChannels) {
      browser.executeScript("useDataChannels()");
    }

    // Setting IceServer (if necessary)
    String iceServerJsFunction = candidateType.getJsFunction();
    log.debug("Setting IceServer: {}", iceServerJsFunction);
    if (iceServerJsFunction != null) {
      browser.executeScript(iceServerJsFunction);
    }

    // Setting MediaConstraints (if necessary)
    String channelJsFunction = channel.getJsFunction();
    if (channelJsFunction != null) {
      browser.executeScript(channelJsFunction);
    }

    // Execute JavaScript kurentoUtils.WebRtcPeer
    browser.executeScript(mode.getJsFunction());

    // SDP offer/answer
    final CountDownLatch latch = new CountDownLatch(1);
    Thread t2 = new Thread() {
      @Override
      public void run() {
        // Wait to valid sdpOffer
        String sdpOffer = (String) browser.executeScriptAndWaitOutput("return sdpOffer;");

        log.debug("SDP offer: {}", sdpOffer);
        String sdpAnswer = webRtcConfigurer.processOffer(sdpOffer);
        log.debug("SDP answer: {}", sdpAnswer);

        // Encoding in Base64 to avoid parsing errors in JavaScript
        sdpAnswer = new String(Base64.encodeBase64(sdpAnswer.getBytes()));

        // Process sdpAnswer
        browser.executeScript("processSdpAnswer('" + sdpAnswer + "');");

        latch.countDown();
      }
    };
    t2.start();

    if (!latch.await(browser.getTimeout(), TimeUnit.SECONDS)) {
      t1.interrupt();
      t1.stop();
      t2.interrupt();
      t2.stop();
      throw new KurentoException(
          "ICE negotiation not finished in " + browser.getTimeout() + " seconds");
    }
  }

  protected void initWebRtc(final WebRtcConfigurer webRtcConfigurer, final WebRtcChannel channel,
      final WebRtcMode mode) throws InterruptedException {
    initWebRtc(webRtcConfigurer, channel, mode, WebRtcCandidateType.ALL, false);
  }

  /*
   * reload
   */
  public void reload() throws IOException {
    browser.reload();
    browser.injectKurentoTestJs();
    browser.executeScriptAndWaitOutput("return kurentoTest;");
    setBrowser(browser);
  }

  /*
   * addTestName
   */
  public void addTestName(String testName) {
    try {
      browser.executeScript("addTestName('" + testName + "');");
    } catch (WebDriverException we) {
      log.warn(we.getMessage());
    }
  }

  /*
   * appendStringToTitle
   */
  public void appendStringToTitle(String webRtcMode) {
    try {
      browser.executeScript("appendStringToTitle('" + webRtcMode + "');");
    } catch (WebDriverException we) {
      log.warn(we.getMessage());
    }
  }

}
