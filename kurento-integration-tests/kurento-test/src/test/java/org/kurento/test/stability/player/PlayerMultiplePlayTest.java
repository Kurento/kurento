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
import org.kurento.commons.exception.KurentoException;
import org.kurento.commons.testing.SystemStabilityTests;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.config.VideoFormat;
import org.kurento.test.functional.player.SimplePlayer;

/**
 * Test of stability for a PlayerEndpoint (play many times different videos). </p> Media
 * Pipeline(s):
 * <ul>
 * <li>PlayerEndpoint -> WebRtcEndpoint</li>
 * </ul>
 * Browser(s):
 * <ul>
 * <li>Chrome</li>
 * <li>Firefox</li>
 * </ul>
 * Test logic:
 * <ol>
 * <li>(KMS) PlayerEndpoint reads different media sources (HTTP/FILE) and different format (WEBM,
 * OGV, MOV, MP4, MKV, AVI, 3GP) and connects to a WebRtcEndpoint</li>
 * <li>(Browser) WebRtcPeer in rcv-only receives media. WebRtcPeer can be configured to receive both
 * video and audio, only video, or only audio</li>
 * </ol>
 * Main assertion(s):
 * <ul>
 * <li>Playing event should be received in remote video tag</li>
 * <li>The color of the received video should be as expected</li><
 * <li>EOS event should arrive to player</li>
 * <li>Play time in remote video should be as expected</li>
 * </ul>
 * Secondary assertion(s):
 * <ul>
 * <li>--</li>
 * </ul>
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
  public void testPlayerMultiplePlay() {
    Protocol[] protocols = { HTTP, FILE };
    VideoFormat[] videoFormats = { THIRDGP, AVI, MKV, MOV, MP4, OGV, WEBM };
    WebRtcChannel[] webRtcChannels = { AUDIO_AND_VIDEO, AUDIO_ONLY, VIDEO_ONLY };
    int numError = 0;
    Throwable t1 = null;

    for (Protocol protocol : protocols) {
      for (VideoFormat videoFormat : videoFormats) {
        for (WebRtcChannel webRtcChannel : webRtcChannels) {
          do {
            try {
              testPlayerWithSmallFile(protocol, videoFormat, webRtcChannel);
              getPage().reload();
            } catch (Throwable t) {
              getPage().reload();
              numError++;
              if (numError > 2) {
                throw new KurentoException("2 Exceptions happens: " + t1.getClass().getName() + " "
                    + t1.getMessage() + " and " + t.getClass().getName() + " " + t.getMessage());
              } else {
                t1 = t;
              }
              continue;
            }
            break;
          } while (true);
        }
      }
    }
  }

}
