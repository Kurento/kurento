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
 * <br/>
 * 
 * Media Pipeline(s): <br>
 * · 4xPlayerEndpoint -> Composite -> RecorderEndpoint <br>
 * · PlayerEndpoint -> WebRtcEndpoint <br>
 * 
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 * 
 * Test logic: <br>
 * 1. (KMS) Media server implements a grid with the media from 4 PlayerEndpoints and it records the
 * grid using the RecorderEndpoint. <br>
 * 2. (KMS) Media server implements a player to reproduce the video recorded in step 1. <br>
 * 3. (Browser) WebRtcPeer in rcv-only receives media <br>
 * 
 * Main assertion(s): <br>
 * · Color of the video should be the expected in the recorded video (red, green, blue, and white)
 * <br>
 * 
 * Secondary assertion(s): <br>
 * · Playing event should be received in remote video tag <br>
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

    PlayerEndpoint playerRed = new PlayerEndpoint.Builder(mp,
        "http://files.kurento.org/video/30sec/red.webm").build();
    PlayerEndpoint playerGreen = new PlayerEndpoint.Builder(mp,
        "http://files.kurento.org/video/30sec/green.webm").build();
    PlayerEndpoint playerBlue = new PlayerEndpoint.Builder(mp,
        "http://files.kurento.org/video/30sec/blue.webm").build();
    PlayerEndpoint playerWhite = new PlayerEndpoint.Builder(mp,
        "http://files.kurento.org/video/30sec/white.webm").build();

    Composite composite = new Composite.Builder(mp).build();
    HubPort hubPort1 = new HubPort.Builder(composite).build();
    HubPort hubPort2 = new HubPort.Builder(composite).build();
    HubPort hubPort3 = new HubPort.Builder(composite).build();
    HubPort hubPort4 = new HubPort.Builder(composite).build();
    HubPort hubPort5 = new HubPort.Builder(composite).build();
    String recordingFile = getDefaultOutputFile(EXTENSION_WEBM);
    RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp, Protocol.FILE + recordingFile)
        .build();

    playerRed.connect(hubPort1);
    playerGreen.connect(hubPort2);
    playerBlue.connect(hubPort3);
    playerWhite.connect(hubPort4);

    hubPort5.connect(recorderEP);

    playerRed.play();
    playerGreen.play();
    playerBlue.play();
    playerWhite.play();

    recorderEP.record();

    Thread.sleep(RECORDTIME * 1000);

    recorderEP.stop();

    playerRed.stop();
    playerGreen.stop();
    playerBlue.stop();
    playerWhite.stop();

    // Media Pipeline #2
    MediaPipeline mp2 = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEP2 = new PlayerEndpoint.Builder(mp2, Protocol.FILE + recordingFile)
        .build();
    WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp2).build();
    playerEP2.connect(webRtcEP2);

    // Playing the recording
    launchBrowser(mp, webRtcEP2, playerEP2, null, EXPECTED_VIDEO_CODEC_WEBM,
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
