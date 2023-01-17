
package org.kurento.test.utils;

import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.kurento.test.browser.WebRtcTestPage;
import org.kurento.test.monitor.PeerConnectionStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TimerTask for checking that packets received are correct when there is audio
 *
 * @author rbenitez (rbenitez@gsyc.es)
 * @since 6.5.1
 */
public class CheckAudioTimerTask extends TimerTask {

  public static Logger log = LoggerFactory.getLogger(CheckAudioTimerTask.class);

  private final CountDownLatch errorContinuityAudiolatch;
  private final WebRtcTestPage page;
  private long lastPacketsReceived = 0;
  private long lastTimestamp = 0;
  private long currentPacketsReceived = 0;
  private long currentTimestamp = 0;
  private long diffTimestamp = 0;
  private int count = 0;
  private double packetsNoReceived = 0;

  public CheckAudioTimerTask(CountDownLatch errorContinuityAudiolatch, WebRtcTestPage page) {
    this.errorContinuityAudiolatch = errorContinuityAudiolatch;
    this.page = page;
  }

  @Override
  public void run() {

    PeerConnectionStats stats = page.getRtcStats();
    if (count != 0) {
      lastPacketsReceived = currentPacketsReceived;
      lastTimestamp = currentTimestamp;
    }

    currentPacketsReceived = page.getPeerConnAudioPacketsRecv(stats);
    currentTimestamp = page.getPeerConnAudioInboundTimestamp(stats);
    diffTimestamp = currentTimestamp - lastTimestamp;
    count++;

    if (lastTimestamp != 0) {
      log.debug("Total audio packets received:{} in {} ms",
          (currentPacketsReceived - lastPacketsReceived), diffTimestamp);
    }

    if (((currentPacketsReceived - lastPacketsReceived) == 0) && (lastTimestamp != 0)) {
      // Packets that must be received in (currentTimestamp - lastTimestamp)
      // Assume that 50 packets/second are received
      double packetsMustReceive = (diffTimestamp * 50.0) / 1000.0;
      packetsNoReceived = packetsNoReceived + packetsMustReceive;
      log.warn("PacketsNoReceived: {}", packetsNoReceived);
    } else {
      // Set 0 because we are looking for the continuity of the audio, and
      // if (current -last) > 0 --> it receives audio packets again
      packetsNoReceived = 0;
    }

    if (packetsNoReceived >= 200) {
      log.warn("PacketsNoReceived >= 200");
      errorContinuityAudiolatch.countDown();
    }
  }
}
