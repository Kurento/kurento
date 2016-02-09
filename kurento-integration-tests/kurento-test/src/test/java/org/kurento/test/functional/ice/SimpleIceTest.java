
package org.kurento.test.functional.ice;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.kurento.client.EventListener;
import org.kurento.client.MediaFlowInStateChangeEvent;
import org.kurento.client.MediaFlowOutStateChangeEvent;
import org.kurento.client.MediaFlowState;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcCandidateType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcIpvMode;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.Protocol;

/**
 * Base for player tests.
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.3.1
 */
public class SimpleIceTest extends FunctionalTest {

  public void initTestSendRecv(WebRtcChannel webRtcChannel, WebRtcIpvMode webRtcIpvMode,
      WebRtcCandidateType webRtcCandidateType) throws InterruptedException {
    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(mp).build();
    webRtcEndpoint.connect(webRtcEndpoint);

    final CountDownLatch eosLatch = new CountDownLatch(1);

    webRtcEndpoint
        .addMediaFlowOutStateChangeListener(new EventListener<MediaFlowOutStateChangeEvent>() {

          @Override
          public void onEvent(MediaFlowOutStateChangeEvent event) {
            if (event.getState().equals(MediaFlowState.FLOWING)) {
              eosLatch.countDown();
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

    // Release Media Pipeline
    mp.release();
  }

  public void initTestRcvOnly(WebRtcChannel webRtcChannel, WebRtcIpvMode webRtcIpvMode,
      WebRtcCandidateType webRtcCandidateType, String nameMedia) throws InterruptedException {

    String mediaUrl = getMediaUrl(Protocol.HTTP, nameMedia);
    MediaPipeline mp = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp = new PlayerEndpoint.Builder(mp, mediaUrl).build();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).build();
    playerEp.connect(webRtcEp);

    final CountDownLatch eosLatch = new CountDownLatch(1);

    webRtcEp.addMediaFlowInStateChangeListener(new EventListener<MediaFlowInStateChangeEvent>() {

      @Override
      public void onEvent(MediaFlowInStateChangeEvent event) {
        if (event.getState().equals(MediaFlowState.FLOWING)) {
          eosLatch.countDown();
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

    // Release Media Pipeline
    mp.release();
  }

  public void initTestSendOnly(WebRtcChannel webRtcChannel, WebRtcIpvMode webRtcIpvMode,
      WebRtcCandidateType webRtcCandidateType) throws InterruptedException {

    MediaPipeline mp = kurentoClient.createMediaPipeline();
    WebRtcEndpoint webRtcEpSendOnly = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEpRcvOnly = new WebRtcEndpoint.Builder(mp).build();

    webRtcEpSendOnly.connect(webRtcEpRcvOnly);

    final CountDownLatch eosLatch = new CountDownLatch(1);

    webRtcEpRcvOnly
        .addMediaFlowInStateChangeListener(new EventListener<MediaFlowInStateChangeEvent>() {

          @Override
          public void onEvent(MediaFlowInStateChangeEvent event) {
            if (event.getState().equals(MediaFlowState.FLOWING)) {
              eosLatch.countDown();
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

    // Release Media Pipeline
    mp.release();
  }

}
