/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

package org.kurento.test.functional.ice;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.config.TestConfiguration.TEST_ICE_CANDIDATE_KMS_TYPE;
import static org.kurento.test.config.TestConfiguration.TEST_ICE_CANDIDATE_SELENIUM_TYPE;
import static org.kurento.test.config.TestConfiguration.TEST_KMS_TRANSPORT;
import static org.kurento.test.config.TestConfiguration.TEST_SELENIUM_TRANSPORT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.kurento.client.EventListener;
import org.kurento.client.IceComponentStateChangedEvent;
import org.kurento.client.MediaFlowInStateChangedEvent;
import org.kurento.client.MediaFlowOutStateChangedEvent;
import org.kurento.client.MediaFlowState;
import org.kurento.client.MediaPipeline;
import org.kurento.client.NewCandidatePairSelectedEvent;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.browser.WebRtcCandidateType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcIpvMode;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.Protocol;
import org.kurento.test.docker.TransportMode;
import org.kurento.test.functional.player.FunctionalPlayerTest;

/**
 * Base for player tests.
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.3.1
 */
public class SimpleIceTest extends FunctionalPlayerTest {

  private List<Candidate> localCandidate;
  private List<Candidate> remoteCandidate;

  private List<Candidate> kmsCandidateType;
  private List<Candidate> seleniumCandidateType;

  protected WebRtcCandidateType getCandidateType(String candidate) {
    String candidateType = candidate.split("typ")[1].split(" ")[1];
    return WebRtcCandidateType.find(candidateType);
  }

  protected TransportMode getTransportMode(String candidate) {
    if (candidate.toUpperCase().contains(TransportMode.UDP.toString())) {
      return TransportMode.UDP;
    } else if (candidate.toUpperCase().contains(TransportMode.TCP.toString())) {
      return TransportMode.TCP;
    }
    return null;
  }

  protected void addCandidates(NewCandidatePairSelectedEvent event) {
    Candidate lCandidate =
        new Candidate(getCandidateType(event.getCandidatePair().getLocalCandidate()),
            getTransportMode(event.getCandidatePair().getLocalCandidate()));

    Candidate rCandidate =
        new Candidate(getCandidateType(event.getCandidatePair().getRemoteCandidate()),
            getTransportMode(event.getCandidatePair().getRemoteCandidate()));

    if (WebRtcCandidateType.PRFLX.equals(lCandidate.getWebRtcCandidateType())) {
      lCandidate.setWebRtcCandidateType(WebRtcCandidateType.SRFLX);
    }

    if (WebRtcCandidateType.PRFLX.equals(rCandidate.getWebRtcCandidateType())) {
      rCandidate.setWebRtcCandidateType(WebRtcCandidateType.SRFLX);
    }

    localCandidate.add(lCandidate);
    remoteCandidate.add(rCandidate);

    if (getProperty(TEST_ICE_CANDIDATE_KMS_TYPE) != null) {
      kmsCandidateType.add(new Candidate(
          WebRtcCandidateType.find(getProperty(TEST_ICE_CANDIDATE_KMS_TYPE).toLowerCase()),
          TransportMode.find(getProperty(TEST_KMS_TRANSPORT))));
    }

    if (getProperty(TEST_ICE_CANDIDATE_SELENIUM_TYPE) != null) {
      seleniumCandidateType.add(new Candidate(
          WebRtcCandidateType.find(getProperty(TEST_ICE_CANDIDATE_SELENIUM_TYPE).toLowerCase()),
          TransportMode.find(getProperty(TEST_SELENIUM_TRANSPORT))));
    }
  }

  protected void initLists() {
    localCandidate = new ArrayList<Candidate>();
    remoteCandidate = new ArrayList<Candidate>();
    kmsCandidateType = new ArrayList<Candidate>();
    seleniumCandidateType = new ArrayList<Candidate>();
  }

  protected void saveGstreamerDot(String prefix, MediaPipeline mp) {
    if (mp != null) {
      String gstreamerDot = mp.getGstreamerDot();
      String pipelineName = mp.getName();
      String gstreamerDotFile = getDefaultOutputFile(prefix + pipelineName);
      try {
        FileUtils.writeStringToFile(new File(gstreamerDotFile), gstreamerDot);
      } catch (IOException e) {
        log.error("Problem saving GstreamerDot with prefix: {} and pipeline: {} in path: {} ",
            prefix, pipelineName, gstreamerDotFile);
        e.printStackTrace();
      }
    }
  }

  public void initTestSendRecv(WebRtcChannel webRtcChannel, WebRtcIpvMode webRtcIpvMode,
      WebRtcCandidateType webRtcCandidateType) throws InterruptedException {

    initLists();

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
    webRtcEndpoint.connect(webRtcEndpoint);

    final CountDownLatch eosLatch = new CountDownLatch(1);

    webRtcEndpoint
        .addIceComponentStateChangedListener(new EventListener<IceComponentStateChangedEvent>() {

          @Override
          public void onEvent(IceComponentStateChangedEvent event) {
            log.debug("IceComponentStateChanged State: {} Source: {} Type: {} StreamId: {}",
                event.getState(), event.getSource(), event.getType(), event.getStreamId());
          }
        });

    webRtcEndpoint
        .addMediaFlowOutStateChangedListener(new EventListener<MediaFlowOutStateChangedEvent>() {

          @Override
          public void onEvent(MediaFlowOutStateChangedEvent event) {
            if (event.getState().equals(MediaFlowState.FLOWING)) {
              eosLatch.countDown();
            }
          }
        });

    webRtcEndpoint
        .addNewCandidatePairSelectedListener(new EventListener<NewCandidatePairSelectedEvent>() {

          @Override
          public void onEvent(NewCandidatePairSelectedEvent event) {
            log.debug(
                "SendRecv -> New Candidate Pair Selected: \nStream: {} \nLocal: {} \nRemote: {}",
                event.getCandidatePair().getStreamID(),
                event.getCandidatePair().getLocalCandidate(),
                event.getCandidatePair().getRemoteCandidate());

            addCandidates(event);
          }
        });

    // Test execution
    getPage(0).subscribeEvents("playing");
    getPage(0).initWebRtc(webRtcEndpoint, webRtcChannel, WebRtcMode.SEND_RCV, webRtcIpvMode,
        webRtcCandidateType);

    // Assertions
    saveGstreamerDot("-before-waiting-FlowingOut-event-", mp);
    Assert.assertTrue("Not received FLOWING OUT event in webRtcEp:" + webRtcChannel,
        eosLatch.await(getPage(0).getTimeout(), TimeUnit.SECONDS));

    saveGstreamerDot("-before-waiting-player-event-", mp);
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage(0).waitForEvent("playing"));

    // For the moment, these assertions are not necessary
    // Assert.assertEquals("Local candidate type (KMS) is wrong. It waits "
    // + kmsCandidateType.get(0).getWebRtcCandidateType() + " and finds "
    // + localCandidate.get(0).getWebRtcCandidateType(), kmsCandidateType.get(0)
    // .getWebRtcCandidateType(), localCandidate.get(0).getWebRtcCandidateType());
    //
    // if (WebRtcCandidateType.RELAY.toString().equals(webRtcCandidateType)) {
    // Assert.assertEquals("Local candidate transport (KMS) is wrong. It waits "
    // + kmsCandidateType.get(0).getTransportMode() + " and finds "
    // + localCandidate.get(0).getTransportMode(), kmsCandidateType.get(0).getTransportMode(),
    // localCandidate.get(0).getTransportMode());
    // }
    //
    // Assert.assertEquals("Remote candidate type (SELENIUM) is wrong. It waits "
    // + seleniumCandidateType.get(0).getWebRtcCandidateType() + " and finds "
    // + remoteCandidate.get(0).getWebRtcCandidateType(), seleniumCandidateType.get(0)
    // .getWebRtcCandidateType(), remoteCandidate.get(0).getWebRtcCandidateType());
    //
    // if (WebRtcCandidateType.RELAY.toString().equals(webRtcCandidateType)) {
    // Assert.assertEquals("Remote candidate transport (SELENIUM) is wrong. It waits "
    // + seleniumCandidateType.get(0).getTransportMode() + " and finds "
    // + remoteCandidate.get(0).getTransportMode(), seleniumCandidateType.get(0)
    // .getTransportMode(), remoteCandidate.get(0).getTransportMode());
    // }

    // Release Media Pipeline
    mp.release();
  }

  public void initTestRcvOnly(WebRtcChannel webRtcChannel, WebRtcIpvMode webRtcIpvMode,
      WebRtcCandidateType webRtcCandidateType, String nameMedia) throws InterruptedException {

    initLists();

    String mediaUrl = getMediaUrl(Protocol.FILE, nameMedia);
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp = new PlayerEndpoint.Builder(mp, mediaUrl).build();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();
    playerEp.connect(webRtcEp);

    final CountDownLatch eosLatch = new CountDownLatch(1);

    webRtcEp.addIceComponentStateChangedListener(new EventListener<IceComponentStateChangedEvent>() {

      @Override
      public void onEvent(IceComponentStateChangedEvent event) {
        log.debug("IceComponentStateChanged State: {} Source: {} Type: {} StreamId: {}",
            event.getState(), event.getSource(), event.getType(), event.getStreamId());
      }
    });

    webRtcEp.addMediaFlowInStateChangedListener(new EventListener<MediaFlowInStateChangedEvent>() {

      @Override
      public void onEvent(MediaFlowInStateChangedEvent event) {
        if (event.getState().equals(MediaFlowState.FLOWING)) {
          eosLatch.countDown();
        }
      }
    });

    webRtcEp
        .addNewCandidatePairSelectedListener(new EventListener<NewCandidatePairSelectedEvent>() {

          @Override
          public void onEvent(NewCandidatePairSelectedEvent event) {
            log.debug(
                "RecvOnly -> New Candidate Pair Selected: \nStream: {} \nLocal: {} \nRemote: {}",
                event.getCandidatePair().getStreamID(),
                event.getCandidatePair().getLocalCandidate(),
                event.getCandidatePair().getRemoteCandidate());

            addCandidates(event);
          }
        });

    // Test execution
    getPage(0).subscribeEvents("playing");
    getPage(0).initWebRtc(webRtcEp, webRtcChannel, WebRtcMode.RCV_ONLY, webRtcIpvMode,
        webRtcCandidateType);
    playerEp.play();

    // Assertions
    saveGstreamerDot("-before-waiting-FlowingIn-event-", mp);
    Assert.assertTrue(
        "Not received FLOWING IN event in webRtcEp: " + mediaUrl + " " + webRtcChannel,
        eosLatch.await(getPage(0).getTimeout(), TimeUnit.SECONDS));

    saveGstreamerDot("-before-waiting-player-event-", mp);
    Assert.assertTrue(
        "Not received media (timeout waiting playing event): " + mediaUrl + " " + webRtcChannel,
        getPage(0).waitForEvent("playing"));

    // For the moment, these assertions are not necessary
    // Assert.assertEquals("Local candidate type (KMS) is wrong. It waits "
    // + kmsCandidateType.get(0).getWebRtcCandidateType() + " and finds "
    // + localCandidate.get(0).getWebRtcCandidateType(), kmsCandidateType.get(0)
    // .getWebRtcCandidateType(), localCandidate.get(0).getWebRtcCandidateType());
    //
    // if (WebRtcCandidateType.RELAY.toString().equals(webRtcCandidateType)) {
    // Assert.assertEquals("Local candidate transport (KMS) is wrong. It waits "
    // + kmsCandidateType.get(0).getTransportMode() + " and finds "
    // + localCandidate.get(0).getTransportMode(), kmsCandidateType.get(0).getTransportMode(),
    // localCandidate.get(0).getTransportMode());
    // }
    //
    // Assert.assertEquals("Remote candidate type (SELENIUM) is wrong. It waits "
    // + seleniumCandidateType.get(0).getWebRtcCandidateType() + " and finds "
    // + remoteCandidate.get(0).getWebRtcCandidateType(), seleniumCandidateType.get(0)
    // .getWebRtcCandidateType(), remoteCandidate.get(0).getWebRtcCandidateType());
    //
    // if (WebRtcCandidateType.RELAY.toString().equals(webRtcCandidateType)) {
    // Assert.assertEquals("Remote candidate transport (SELENIUM) is wrong. It waits "
    // + seleniumCandidateType.get(0).getTransportMode() + " and finds "
    // + remoteCandidate.get(0).getTransportMode(), seleniumCandidateType.get(0)
    // .getTransportMode(), remoteCandidate.get(0).getTransportMode());
    // }

    // Release Media Pipeline
    mp.release();
  }

  public void initTestSendOnly(WebRtcChannel webRtcChannel, WebRtcIpvMode webRtcIpvMode,
      WebRtcCandidateType webRtcCandidateType) throws InterruptedException {

    initLists();

    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEpSendOnly = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEpRcvOnly = new WebRtcEndpoint.Builder(mp).build();

    webRtcEpSendOnly.connect(webRtcEpRcvOnly);

    final CountDownLatch eosLatch = new CountDownLatch(1);

    webRtcEpSendOnly
        .addIceComponentStateChangedListener(new EventListener<IceComponentStateChangedEvent>() {

          @Override
          public void onEvent(IceComponentStateChangedEvent event) {
            log.debug(
                "webRtcEpSendOnly: IceComponentStateChanged State: {} Source: {} Type: {} StreamId: {}",
                event.getState(), event.getSource(), event.getType(), event.getStreamId());
          }
        });

    webRtcEpSendOnly
        .addNewCandidatePairSelectedListener(new EventListener<NewCandidatePairSelectedEvent>() {

          @Override
          public void onEvent(NewCandidatePairSelectedEvent event) {
            log.debug(
                "SendOnly (webRtcEpSendOnly) -> New Candidate Pair Selected: \nStream: {} \nLocal: {} \nRemote: {}",
                event.getCandidatePair().getStreamID(),
                event.getCandidatePair().getLocalCandidate(),
                event.getCandidatePair().getRemoteCandidate());
          }
        });

    webRtcEpRcvOnly
        .addIceComponentStateChangedListener(new EventListener<IceComponentStateChangedEvent>() {

          @Override
          public void onEvent(IceComponentStateChangedEvent event) {
            log.debug(
                "webRtcEpRcvOnly: IceComponentStateChanged State: {} Source: {} Type: {} StreamId: {}",
                event.getState(), event.getSource(), event.getType(), event.getStreamId());
          }
        });

    webRtcEpRcvOnly
        .addMediaFlowInStateChangedListener(new EventListener<MediaFlowInStateChangedEvent>() {

          @Override
          public void onEvent(MediaFlowInStateChangedEvent event) {
            if (event.getState().equals(MediaFlowState.FLOWING)) {
              eosLatch.countDown();
            }
          }
        });

    webRtcEpRcvOnly
        .addNewCandidatePairSelectedListener(new EventListener<NewCandidatePairSelectedEvent>() {

          @Override
          public void onEvent(NewCandidatePairSelectedEvent event) {
            log.debug(
                "SendOnly (webRtcEpRcvOnly) -> New Candidate Pair Selected: \nStream: {} \nLocal: {} \nRemote: {}",
                event.getCandidatePair().getStreamID(),
                event.getCandidatePair().getLocalCandidate(),
                event.getCandidatePair().getRemoteCandidate());

            addCandidates(event);
          }
        });

    // Test execution
    getPage(1).subscribeEvents("playing");
    getPage(0).initWebRtc(webRtcEpSendOnly, webRtcChannel, WebRtcMode.SEND_ONLY, webRtcIpvMode,
        webRtcCandidateType);
    getPage(1).initWebRtc(webRtcEpRcvOnly, webRtcChannel, WebRtcMode.RCV_ONLY, webRtcIpvMode,
        webRtcCandidateType);

    // Assertions
    saveGstreamerDot("-before-waiting-FlowingIn-event-", mp);
    Assert.assertTrue("Not received FLOWING IN event in webRtcEpRcvOnly: " + webRtcChannel,
        eosLatch.await(getPage(1).getTimeout(), TimeUnit.SECONDS));

    saveGstreamerDot("-before-waiting-player-event-", mp);
    Assert.assertTrue("Not received media (timeout waiting playing event)",
        getPage(1).waitForEvent("playing"));

    // For the moment, these assertions are not necessary
    // Assert.assertEquals("Local candidate type (KMS) is wrong. It waits "
    // + kmsCandidateType.get(0).getWebRtcCandidateType() + " and finds "
    // + localCandidate.get(0).getWebRtcCandidateType(), kmsCandidateType.get(0)
    // .getWebRtcCandidateType(), localCandidate.get(0).getWebRtcCandidateType());
    //
    // if (WebRtcCandidateType.RELAY.toString().equals(webRtcCandidateType)) {
    // Assert.assertEquals("Local candidate transport (KMS) is wrong. It waits "
    // + kmsCandidateType.get(0).getTransportMode() + " and finds "
    // + localCandidate.get(0).getTransportMode(), kmsCandidateType.get(0).getTransportMode(),
    // localCandidate.get(0).getTransportMode());
    // }
    //
    // Assert.assertEquals("Remote candidate type (SELENIUM) is wrong. It waits "
    // + seleniumCandidateType.get(0).getWebRtcCandidateType() + " and finds "
    // + remoteCandidate.get(0).getWebRtcCandidateType(), seleniumCandidateType.get(0)
    // .getWebRtcCandidateType(), remoteCandidate.get(0).getWebRtcCandidateType());
    //
    // if (WebRtcCandidateType.RELAY.toString().equals(webRtcCandidateType)) {
    // Assert.assertEquals("Remote candidate transport (SELENIUM) is wrong. It waits "
    // + seleniumCandidateType.get(0).getTransportMode() + " and finds "
    // + remoteCandidate.get(0).getTransportMode(), seleniumCandidateType.get(0)
    // .getTransportMode(), remoteCandidate.get(0).getTransportMode());
    // }

    // Release Media Pipeline
    mp.release();
  }

  private class Candidate {
    private WebRtcCandidateType webRtcCandidateType;
    private TransportMode transportMode;

    public Candidate(WebRtcCandidateType webRtcCandidateType, TransportMode transportMode) {
      this.webRtcCandidateType = webRtcCandidateType;
      this.transportMode = transportMode;
    }

    public WebRtcCandidateType getWebRtcCandidateType() {
      return webRtcCandidateType;
    }

    public void setWebRtcCandidateType(WebRtcCandidateType webRtcCandidateType) {
      this.webRtcCandidateType = webRtcCandidateType;
    }

    public TransportMode getTransportMode() {
      return transportMode;
    }
  }

}
