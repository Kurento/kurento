
package org.kurento.test.functional.ice;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.config.TestConfiguration.TEST_ICE_CANDIDATE_KMS_TYPE;
import static org.kurento.test.config.TestConfiguration.TEST_ICE_CANDIDATE_SELENIUM_TYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.kurento.client.EventListener;
import org.kurento.client.MediaFlowInStateChangeEvent;
import org.kurento.client.MediaFlowOutStateChangeEvent;
import org.kurento.client.MediaFlowState;
import org.kurento.client.MediaPipeline;
import org.kurento.client.NewCandidatePairSelectedEvent;
import org.kurento.client.OnIceComponentStateChangedEvent;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.browser.WebRtcCandidateType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcIpvMode;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.Protocol;
import org.kurento.test.functional.player.FunctionalPlayerTest;

/**
 * Base for player tests.
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.3.1
 */
public class SimpleIceTest extends FunctionalPlayerTest {

  private List<WebRtcCandidateType> localCandidate;
  private List<WebRtcCandidateType> remoteCandidate;

  private List<WebRtcCandidateType> kmsCandidateType;
  private List<WebRtcCandidateType> seleniumCandidateType;

  protected WebRtcCandidateType getCandidateType(String candidate) {
    String candidateType = candidate.split("typ")[1].split(" ")[1];
    return WebRtcCandidateType.find(candidateType);
  }

  public void initTestSendRecv(WebRtcChannel webRtcChannel, WebRtcIpvMode webRtcIpvMode,
      WebRtcCandidateType webRtcCandidateType) throws InterruptedException {

    localCandidate = new ArrayList<WebRtcCandidateType>();
    remoteCandidate = new ArrayList<WebRtcCandidateType>();
    kmsCandidateType = new ArrayList<WebRtcCandidateType>();
    seleniumCandidateType = new ArrayList<WebRtcCandidateType>();

    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
    webRtcEndpoint.connect(webRtcEndpoint);

    final CountDownLatch eosLatch = new CountDownLatch(1);

    webRtcEndpoint
        .addOnIceComponentStateChangedListener(new EventListener<OnIceComponentStateChangedEvent>() {

          @Override
          public void onEvent(OnIceComponentStateChangedEvent event) {
            log.info("OnIceComponentStateChanged State: {} Source: {} Type: {} StreamId: {}",
                event.getState(), event.getSource(), event.getType(), event.getStreamId());
          }
        });

    webRtcEndpoint
        .addMediaFlowOutStateChangeListener(new EventListener<MediaFlowOutStateChangeEvent>() {

          @Override
          public void onEvent(MediaFlowOutStateChangeEvent event) {
            if (event.getState().equals(MediaFlowState.FLOWING)) {
              eosLatch.countDown();
            }
          }
        });

    webRtcEndpoint
        .addNewCandidatePairSelectedListener(new EventListener<NewCandidatePairSelectedEvent>() {

          @Override
          public void onEvent(NewCandidatePairSelectedEvent event) {
            log.info(
                "SendRecv -> New Candidate Pair Selected: \nStream: {} \nLocal: {} \nRemote: {}",
                event.getCandidatePair().getStreamID(), event.getCandidatePair()
                    .getLocalCandidate(), event.getCandidatePair().getRemoteCandidate());

            WebRtcCandidateType lCandidate =
                getCandidateType(event.getCandidatePair().getLocalCandidate());
            WebRtcCandidateType rCandidate =
                getCandidateType(event.getCandidatePair().getRemoteCandidate());

            if (WebRtcCandidateType.PRFLX.equals(lCandidate)) {
              lCandidate = WebRtcCandidateType.SRFLX;
            }

            if (WebRtcCandidateType.PRFLX.equals(rCandidate)) {
              rCandidate = WebRtcCandidateType.SRFLX;
            }

            localCandidate.add(lCandidate);
            remoteCandidate.add(rCandidate);

            if (getProperty(TEST_ICE_CANDIDATE_KMS_TYPE) != null) {
              kmsCandidateType.add(WebRtcCandidateType
                  .find(getProperty(TEST_ICE_CANDIDATE_KMS_TYPE).toLowerCase()));
            }

            if (getProperty(TEST_ICE_CANDIDATE_SELENIUM_TYPE) != null) {
              seleniumCandidateType.add(WebRtcCandidateType.find(getProperty(
                  TEST_ICE_CANDIDATE_SELENIUM_TYPE).toLowerCase()));
            }
          }
        });

    // Test execution
    getPage(0).subscribeEvents("playing");
    getPage(0).initWebRtc(webRtcEndpoint, webRtcChannel, WebRtcMode.SEND_RCV, webRtcIpvMode,
        webRtcCandidateType);

    // Assertions
    Assert.assertTrue("Not received media (timeout waiting playing event)", getPage(0)
        .waitForEvent("playing"));

    Assert.assertTrue("Not received FLOWING OUT event in webRtcEp:" + webRtcChannel,
        eosLatch.await(getPage(0).getTimeout(), TimeUnit.SECONDS));

    log.info("Assert: {} {} {} {}", localCandidate.get(0), kmsCandidateType.get(0),
        remoteCandidate.get(0), seleniumCandidateType.get(0));
    Assert.assertEquals("Local candidate (KMS) is wrong. It waits " + kmsCandidateType.get(0)
        + " and finds " + localCandidate.get(0), kmsCandidateType.get(0), localCandidate.get(0));
    Assert.assertEquals(
        "Remote candidate (SELENIUM) is wrong. It waits " + seleniumCandidateType.get(0) + "",
        seleniumCandidateType.get(0), remoteCandidate.get(0));

    // Release Media Pipeline
    mp.release();
  }

  public void initTestRcvOnly(WebRtcChannel webRtcChannel, WebRtcIpvMode webRtcIpvMode,
      WebRtcCandidateType webRtcCandidateType, String nameMedia) throws InterruptedException {

    localCandidate = new ArrayList<WebRtcCandidateType>();
    remoteCandidate = new ArrayList<WebRtcCandidateType>();
    kmsCandidateType = new ArrayList<WebRtcCandidateType>();
    seleniumCandidateType = new ArrayList<WebRtcCandidateType>();

    String mediaUrl = getMediaUrl(Protocol.FILE, nameMedia);
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp = new PlayerEndpoint.Builder(mp, mediaUrl).build();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();
    playerEp.connect(webRtcEp);

    final CountDownLatch eosLatch = new CountDownLatch(1);

    webRtcEp
        .addOnIceComponentStateChangedListener(new EventListener<OnIceComponentStateChangedEvent>() {

          @Override
          public void onEvent(OnIceComponentStateChangedEvent event) {
            log.info("OnIceComponentStateChanged State: {} Source: {} Type: {} StreamId: {}",
                event.getState(), event.getSource(), event.getType(), event.getStreamId());
          }
        });

    webRtcEp.addMediaFlowInStateChangeListener(new EventListener<MediaFlowInStateChangeEvent>() {

      @Override
      public void onEvent(MediaFlowInStateChangeEvent event) {
        if (event.getState().equals(MediaFlowState.FLOWING)) {
          eosLatch.countDown();
        }
      }
    });

    webRtcEp
        .addNewCandidatePairSelectedListener(new EventListener<NewCandidatePairSelectedEvent>() {

          @Override
          public void onEvent(NewCandidatePairSelectedEvent event) {
            log.info(
                "RecvOnly -> New Candidate Pair Selected: \nStream: {} \nLocal: {} \nRemote: {}",
                event.getCandidatePair().getStreamID(), event.getCandidatePair()
                    .getLocalCandidate(), event.getCandidatePair().getRemoteCandidate());

            WebRtcCandidateType lCandidate =
                getCandidateType(event.getCandidatePair().getLocalCandidate());
            WebRtcCandidateType rCandidate =
                getCandidateType(event.getCandidatePair().getRemoteCandidate());

            if (WebRtcCandidateType.PRFLX.equals(lCandidate)) {
              lCandidate = WebRtcCandidateType.SRFLX;
            }

            if (WebRtcCandidateType.PRFLX.equals(rCandidate)) {
              rCandidate = WebRtcCandidateType.SRFLX;
            }

            localCandidate.add(lCandidate);
            remoteCandidate.add(rCandidate);

            if (getProperty(TEST_ICE_CANDIDATE_KMS_TYPE) != null) {
              kmsCandidateType.add(WebRtcCandidateType
                  .find(getProperty(TEST_ICE_CANDIDATE_KMS_TYPE).toLowerCase()));
            }

            if (getProperty(TEST_ICE_CANDIDATE_SELENIUM_TYPE) != null) {
              seleniumCandidateType.add(WebRtcCandidateType.find(getProperty(
                  TEST_ICE_CANDIDATE_SELENIUM_TYPE).toLowerCase()));
            }
          }
        });

    // Test execution
    getPage(0).subscribeEvents("playing");
    getPage(0).initWebRtc(webRtcEp, webRtcChannel, WebRtcMode.RCV_ONLY, webRtcIpvMode,
        webRtcCandidateType);
    playerEp.play();

    // Assertions
    Assert.assertTrue("Not received media (timeout waiting playing event): " + mediaUrl + " "
        + webRtcChannel, getPage(0).waitForEvent("playing"));

    Assert.assertTrue("Not received FLOWING IN event in webRtcEp: " + mediaUrl + " "
        + webRtcChannel, eosLatch.await(getPage(0).getTimeout(), TimeUnit.SECONDS));

    log.info("Assert: {} {} {} {}", localCandidate.get(0), kmsCandidateType.get(0),
        remoteCandidate.get(0), seleniumCandidateType.get(0));
    Assert.assertEquals("Local candidate (KMS) is wrong. It waits " + kmsCandidateType.get(0)
        + " and finds " + localCandidate.get(0), kmsCandidateType.get(0), localCandidate.get(0));
    Assert.assertEquals(
        "Remote candidate (SELENIUM) is wrong. It waits " + seleniumCandidateType.get(0) + "",
        seleniumCandidateType.get(0), remoteCandidate.get(0));

    // Release Media Pipeline
    mp.release();
  }

  public void initTestSendOnly(WebRtcChannel webRtcChannel, WebRtcIpvMode webRtcIpvMode,
      WebRtcCandidateType webRtcCandidateType) throws InterruptedException {

    localCandidate = new ArrayList<WebRtcCandidateType>();
    remoteCandidate = new ArrayList<WebRtcCandidateType>();
    kmsCandidateType = new ArrayList<WebRtcCandidateType>();
    seleniumCandidateType = new ArrayList<WebRtcCandidateType>();

    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEpSendOnly = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEpRcvOnly = new WebRtcEndpoint.Builder(mp).build();

    webRtcEpSendOnly.connect(webRtcEpRcvOnly);

    final CountDownLatch eosLatch = new CountDownLatch(1);

    webRtcEpSendOnly
        .addOnIceComponentStateChangedListener(new EventListener<OnIceComponentStateChangedEvent>() {

          @Override
          public void onEvent(OnIceComponentStateChangedEvent event) {
            log.info(
                "webRtcEpSendOnly: OnIceComponentStateChanged State: {} Source: {} Type: {} StreamId: {}",
                event.getState(), event.getSource(), event.getType(), event.getStreamId());
          }
        });

    webRtcEpSendOnly
        .addNewCandidatePairSelectedListener(new EventListener<NewCandidatePairSelectedEvent>() {

          @Override
          public void onEvent(NewCandidatePairSelectedEvent event) {
            log.info(
                "SendOnly (webRtcEpSendOnly) -> New Candidate Pair Selected: \nStream: {} \nLocal: {} \nRemote: {}",
                event.getCandidatePair().getStreamID(), event.getCandidatePair()
                    .getLocalCandidate(), event.getCandidatePair().getRemoteCandidate());
          }
        });

    webRtcEpRcvOnly
        .addOnIceComponentStateChangedListener(new EventListener<OnIceComponentStateChangedEvent>() {

          @Override
          public void onEvent(OnIceComponentStateChangedEvent event) {
            log.info(
                "webRtcEpRcvOnly: OnIceComponentStateChanged State: {} Source: {} Type: {} StreamId: {}",
                event.getState(), event.getSource(), event.getType(), event.getStreamId());
          }
        });

    webRtcEpRcvOnly
        .addMediaFlowInStateChangeListener(new EventListener<MediaFlowInStateChangeEvent>() {

          @Override
          public void onEvent(MediaFlowInStateChangeEvent event) {
            if (event.getState().equals(MediaFlowState.FLOWING)) {
              eosLatch.countDown();
            }
          }
        });

    webRtcEpRcvOnly
        .addNewCandidatePairSelectedListener(new EventListener<NewCandidatePairSelectedEvent>() {

          @Override
          public void onEvent(NewCandidatePairSelectedEvent event) {
            log.info(
                "SendOnly (webRtcEpRcvOnly) -> New Candidate Pair Selected: \nStream: {} \nLocal: {} \nRemote: {}",
                event.getCandidatePair().getStreamID(), event.getCandidatePair()
                    .getLocalCandidate(), event.getCandidatePair().getRemoteCandidate());

            WebRtcCandidateType lCandidate =
                getCandidateType(event.getCandidatePair().getLocalCandidate());
            WebRtcCandidateType rCandidate =
                getCandidateType(event.getCandidatePair().getRemoteCandidate());

            if (WebRtcCandidateType.PRFLX.equals(lCandidate)) {
              lCandidate = WebRtcCandidateType.SRFLX;
            }

            if (WebRtcCandidateType.PRFLX.equals(rCandidate)) {
              rCandidate = WebRtcCandidateType.SRFLX;
            }

            localCandidate.add(lCandidate);
            remoteCandidate.add(rCandidate);

            if (getProperty(TEST_ICE_CANDIDATE_KMS_TYPE) != null) {
              kmsCandidateType.add(WebRtcCandidateType
                  .find(getProperty(TEST_ICE_CANDIDATE_KMS_TYPE).toLowerCase()));
            }

            if (getProperty(TEST_ICE_CANDIDATE_SELENIUM_TYPE) != null) {
              seleniumCandidateType.add(WebRtcCandidateType.find(getProperty(
                  TEST_ICE_CANDIDATE_SELENIUM_TYPE).toLowerCase()));
            }
          }
        });

    // Test execution
    getPage(1).subscribeEvents("playing");
    getPage(0).initWebRtc(webRtcEpSendOnly, webRtcChannel, WebRtcMode.SEND_ONLY, webRtcIpvMode,
        webRtcCandidateType);
    getPage(1).initWebRtc(webRtcEpRcvOnly, webRtcChannel, WebRtcMode.RCV_ONLY, webRtcIpvMode,
        webRtcCandidateType);

    // Assertions
    Assert.assertTrue("Not received media (timeout waiting playing event)", getPage(1)
        .waitForEvent("playing"));

    Assert.assertTrue("Not received FLOWING IN event in webRtcEpRcvOnly: " + webRtcChannel,
        eosLatch.await(getPage(1).getTimeout(), TimeUnit.SECONDS));

    log.info("Assert: {} {} {} {}", localCandidate.get(0), kmsCandidateType.get(0),
        remoteCandidate.get(0), seleniumCandidateType.get(0));
    Assert.assertEquals("Local candidate (KMS) is wrong. It waits " + kmsCandidateType.get(0)
        + " and finds " + localCandidate.get(0), kmsCandidateType.get(0), localCandidate.get(0));
    Assert.assertEquals(
        "Remote candidate (SELENIUM) is wrong. It waits " + seleniumCandidateType.get(0) + "",
        seleniumCandidateType.get(0), remoteCandidate.get(0));

    // Release Media Pipeline
    mp.release();
  }

}
