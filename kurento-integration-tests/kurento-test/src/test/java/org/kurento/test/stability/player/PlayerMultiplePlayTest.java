/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package org.kurento.test.stability.player;

import static org.kurento.test.browser.WebRtcChannel.AUDIO_AND_VIDEO;
import static org.kurento.test.browser.WebRtcChannel.AUDIO_ONLY;
import static org.kurento.test.browser.WebRtcChannel.VIDEO_ONLY;
import static org.kurento.test.config.Protocol.FILE;
import static org.kurento.test.config.Protocol.HTTP;
import static org.kurento.test.config.VideoFormat.AVI;
import static org.kurento.test.config.VideoFormat.MKV;
import static org.kurento.test.config.VideoFormat.MOV;
import static org.kurento.test.config.VideoFormat.MP4;
import static org.kurento.test.config.VideoFormat.OGV;
import static org.kurento.test.config.VideoFormat.THIRDGP;
import static org.kurento.test.config.VideoFormat.WEBM;

import java.util.Collection;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.commons.testing.SystemStabilityTests;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.config.VideoFormat;
import org.kurento.test.functional.player.SimplePlayer;

/**
 * Test of stability for a PlayerEndpoint (play many times different videos). <br>
 *
 * Media Pipeline(s): <br>
 * · PlayerEndpoint -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br>
 * 1. (KMS) PlayerEndpoint reads different media sources (HTTP/FILE) and different format (WEBM,
 * OGV, MOV, MP4, MKV, AVI, 3GP) and connects to a WebRtcEndpoint <br>
 * 2. (Browser) WebRtcPeer in rcv-only receives media. WebRtcPeer can be configured to receive both
 * video and audio, only video, or only audio <br>
 *
 * Main assertion(s): <br>
 * · Playing event should be received in remote video tag <br>
 * · The color of the received video should be as expected <br>
 * · EOS event should arrive to player <br>
 * · Play time in remote video should be as expected <br>
 *
 * Secondary assertion(s): <br>
 * -- <br>
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
@Category(SystemStabilityTests.class)
public class PlayerMultiplePlayTest extends SimplePlayer {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromeAndFirefox();
  }

  @Test
  public void testPlayerMultiplePlay() throws Exception {
    Protocol[] protocols = { HTTP, FILE };
    VideoFormat[] videoFormats = { THIRDGP, AVI, MKV, MOV, MP4, OGV, WEBM };
    WebRtcChannel[] webRtcChannels = { AUDIO_AND_VIDEO, AUDIO_ONLY, VIDEO_ONLY };

    for (Protocol protocol : protocols) {
      for (VideoFormat videoFormat : videoFormats) {
        for (WebRtcChannel webRtcChannel : webRtcChannels) {
          testPlayerWithSmallFile(protocol, videoFormat, webRtcChannel);
          getPage().reload();
        }
      }
    }
  }

}
