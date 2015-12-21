
package org.kurento.test.functional.composite;

import java.awt.Color;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Composite;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.functional.recorder.BaseRecorder;

/**
 * Four synthetic videos are played by four PlayerEndpoint and mixed by a Composite. The resulting
 * video is recording using a RecorderEndpoint. The recorded video is played using a PlayerEndpoint.
 * </p>
 * Media Pipeline(s):
 * <ul>
 * <li>4xPlayerEndpoint -> Composite -> RecorderEndpoint</li>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) Media server implements a grid with the media from 4 PlayerEndpoints and it records the
 * grid using the RecorderEndpoint.</li>
 * <li>(KMS) Media server implements a player to reproduce the video recorded in step 1.</li>
 * <li>(Browser) WebRtcPeer in rcv-only receives media</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Color of the video should be the expected in the recorded video (red, green, blue, and white)
 * Endpoint</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * </ul>
 *
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 6.1.1
 */

public class CompositeRecorderTest extends BaseRecorder {

  private static int RECORDTIME = 20; // seconds
  private static int PLAYTIME = 20; // seconds

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testCompositeRecorder() throws Exception {
    // MediaPipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();

    PlayerEndpoint playerRed =
        new PlayerEndpoint.Builder(mp, "http://" + getTestFilesHttpPath() + "/video/30sec/red.webm")
            .build();
    PlayerEndpoint playerGreen = new PlayerEndpoint.Builder(mp,
        "http://" + getTestFilesHttpPath() + "/video/30sec/green.webm").build();
    PlayerEndpoint playerBlue = new PlayerEndpoint.Builder(mp,
        "http://" + getTestFilesHttpPath() + "/video/30sec/blue.webm").build();

    Composite composite = new Composite.Builder(mp).build();
    HubPort hubPort1 = new HubPort.Builder(composite).build();
    HubPort hubPort2 = new HubPort.Builder(composite).build();
    HubPort hubPort3 = new HubPort.Builder(composite).build();

    playerRed.connect(hubPort1);
    playerGreen.connect(hubPort2);
    playerBlue.connect(hubPort3);

    PlayerEndpoint playerWhite = new PlayerEndpoint.Builder(mp,
        "http://" + getTestFilesHttpPath() + "/video/30sec/white.webm").build();
    HubPort hubPort4 = new HubPort.Builder(composite).build();
    playerWhite.connect(hubPort4);

    HubPort hubPort5 = new HubPort.Builder(composite).build();
    String recordingFile = getDefaultOutputFile(EXTENSION_WEBM);
    RecorderEndpoint recorderEp =
        new RecorderEndpoint.Builder(mp, Protocol.FILE + recordingFile).build();
    hubPort5.connect(recorderEp);

    playerRed.play();
    playerGreen.play();
    playerBlue.play();
    playerWhite.play();

    recorderEp.record();

    Thread.sleep(RECORDTIME * 1000);

    recorderEp.stop();

    playerRed.stop();
    playerGreen.stop();
    playerBlue.stop();
    playerWhite.stop();

    // Media Pipeline #2
    MediaPipeline mp2 = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEp2 =
        new PlayerEndpoint.Builder(mp2, Protocol.FILE + recordingFile).build();
    WebRtcEndpoint webRtcEp2 = new WebRtcEndpoint.Builder(mp2).build();
    playerEp2.connect(webRtcEp2);

    // Playing the recording
    launchBrowser(mp, webRtcEp2, playerEp2, null, EXPECTED_VIDEO_CODEC_WEBM,
        EXPECTED_AUDIO_CODEC_WEBM, recordingFile, Color.RED, 0, 0, PLAYTIME);

    // Assertions
    Assert.assertTrue("Upper left part of the video must be red",
        getPage().similarColorAt(Color.RED, 0, 0));
    Assert.assertTrue("Upper right part of the video must be green",
        getPage().similarColorAt(Color.GREEN, 450, 0));
    Assert.assertTrue("Lower left part of the video must be blue",
        getPage().similarColorAt(Color.BLUE, 0, 450));
    Assert.assertTrue("Lower right part of the video must be white",
        getPage().similarColorAt(Color.WHITE, 450, 450));

    // Release Media Pipeline #2
    mp2.release();

    success = true;
  }
}
