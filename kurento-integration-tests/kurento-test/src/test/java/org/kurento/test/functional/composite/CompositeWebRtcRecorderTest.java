package org.kurento.test.functional.composite;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.Composite;
import org.kurento.client.HubPort;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaType;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.browser.Browser;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.functional.recorder.BaseRecorder;

/**
 * Four synthetic videos are played by four WebRtcEndpoint (Connect only audio for thres videos) and
 * mixed by a Composite. The resulting video is recording using a RecorderEndpoint. The recorded
 * video is played using a PlayerEndpoint.<br/>
 * 
 * Media Pipeline(s): <br>
 * · 4xWebRtcEndpoint -> Composite -> RecorderEndpoint <br>
 * · PlayerEndpoint -> WebRtcEndpoint <br>
 * 
 * Browser(s): <br>
 * · Chrome <br>
 * 
 * Test logic: <br>
 * 1. (KMS) Media server mix the media from 4 WebRtcEndpoint and it records the grid using the
 * RecorderEndpoint. Only red color (video from first player) and audio from four players.<br>
 * 2. (KMS) Media server implements a player to reproduce the video recorded in step 1. <br>
 * 3. (Browser) WebRtcPeer in rcv-only receives media <br>
 * 
 * Main assertion(s): <br>
 * · Color of the video should be the expected in the recorded video (red)<br>
 * 
 * Secondary assertion(s): <br>
 * · Playing event should be received in remote video tag <br>
 * 
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 6.1.1
 */

public class CompositeWebRtcRecorderTest extends BaseRecorder {

  private static int PLAYTIME = 10; // seconds

  private static final String BROWSER1 = "browser1";
  private static final String BROWSER2 = "browser2";
  private static final String BROWSER3 = "browser3";
  private static final String BROWSER4 = "browser4";
  private static final String BROWSER5 = "browser5";

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    // Test: 5 local Chrome's
    TestScenario test = new TestScenario();
    test.addBrowser(BROWSER1, new Browser.Builder().browserType(BrowserType.CHROME)
        .webPageType(WebPageType.WEBRTC).scope(BrowserScope.LOCAL).build());
    test.addBrowser(BROWSER2,
        new Browser.Builder().browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
            .scope(BrowserScope.LOCAL).video(getTestFilesPath() + "/video/10sec/red.y4m").build());
    test.addBrowser(BROWSER3,
        new Browser.Builder().browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
            .scope(BrowserScope.LOCAL).video(getTestFilesPath() + "/video/10sec/green.y4m")
            .build());
    test.addBrowser(BROWSER4,
        new Browser.Builder().browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
            .scope(BrowserScope.LOCAL).video(getTestFilesPath() + "/video/10sec/blue.y4m").build());
    test.addBrowser(BROWSER5,
        new Browser.Builder().browserType(BrowserType.CHROME).webPageType(WebPageType.WEBRTC)
            .scope(BrowserScope.LOCAL).video(getTestFilesPath() + "/video/10sec/white.y4m")
            .build());
    return Arrays.asList(new Object[][] { { test } });
  }

  @Test
  public void testCompositeRecorder() throws Exception {

    // MediaPipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();

    WebRtcEndpoint webRtcEPRed = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEPGreen = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEPBlue = new WebRtcEndpoint.Builder(mp).build();
    WebRtcEndpoint webRtcEPWhite = new WebRtcEndpoint.Builder(mp).build();

    Composite composite = new Composite.Builder(mp).build();
    HubPort hubPort1 = new HubPort.Builder(composite).build();
    HubPort hubPort2 = new HubPort.Builder(composite).build();
    HubPort hubPort3 = new HubPort.Builder(composite).build();
    HubPort hubPort4 = new HubPort.Builder(composite).build();
    HubPort hubPort5 = new HubPort.Builder(composite).build();
    String recordingFile = getDefaultOutputFile(EXTENSION_WEBM);
    RecorderEndpoint recorderEP = new RecorderEndpoint.Builder(mp, Protocol.FILE + recordingFile)
        .build();

    webRtcEPRed.connect(hubPort1);
    webRtcEPGreen.connect(hubPort2, MediaType.AUDIO);
    webRtcEPBlue.connect(hubPort3, MediaType.AUDIO);
    webRtcEPWhite.connect(hubPort4, MediaType.AUDIO);

    hubPort5.connect(recorderEP);

    // WebRTC browsers
    getPage(BROWSER2).initWebRtc(webRtcEPRed, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);
    getPage(BROWSER3).initWebRtc(webRtcEPGreen, WebRtcChannel.AUDIO_AND_VIDEO,
        WebRtcMode.SEND_ONLY);
    getPage(BROWSER4).initWebRtc(webRtcEPBlue, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);
    getPage(BROWSER5).initWebRtc(webRtcEPWhite, WebRtcChannel.AUDIO_AND_VIDEO,
        WebRtcMode.SEND_ONLY);

    recorderEP.record();

    Thread.sleep(PLAYTIME * 1000);

    recorderEP.stop();
    mp.release();

    // Media Pipeline #2
    MediaPipeline mp2 = kurentoClient.createMediaPipeline();
    PlayerEndpoint playerEP2 = new PlayerEndpoint.Builder(mp2, Protocol.FILE + recordingFile)
        .build();
    WebRtcEndpoint webRtcEP2 = new WebRtcEndpoint.Builder(mp2).build();
    playerEP2.connect(webRtcEP2);

    // Playing the recorded file
    launchBrowser(mp2, webRtcEP2, playerEP2, null, EXPECTED_VIDEO_CODEC_WEBM,
        EXPECTED_AUDIO_CODEC_WEBM, recordingFile, Color.RED, 0, 0, PLAYTIME);

    // Release Media Pipeline #2
    mp2.release();

    success = true;
  }
}
