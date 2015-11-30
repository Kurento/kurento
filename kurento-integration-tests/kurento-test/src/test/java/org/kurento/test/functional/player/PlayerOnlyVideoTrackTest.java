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
package org.kurento.test.functional.player;

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
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.config.TestScenario;

/**
 * Test of a PlayerEndpoint with different types of media sources (WEBM, OGV,
 * MOV, MP4, MKV, AVI, 3GP ... all with ONLY VIDEO) connected to a
 * WebRtcEndpoint. <br>
 *
 * Media Pipeline(s): <br>
 * · PlayerEndpoint -> WebRtcEndpoint <br>
 *
 * Browser(s): <br>
 * · Chrome <br>
 * · Firefox <br>
 *
 * Test logic: <br>
 * 1. (KMS) PlayerEndpoint reads media source (from HTTP and FILE) and connects
 * to a WebRtcEndpoint <br>
 * 2. (Browser) WebRtcPeer in rcv-only receives media <br>
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
public class PlayerOnlyVideoTrackTest extends FunctionalPlayerTest {

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChromeAndFirefox();
	}

	@Test
	public void testPlayerWebRtcVideoOnlyHttp3gp() throws Exception {
		testPlayerWithSmallFileVideoOnly(HTTP, THIRDGP, VIDEO_ONLY);
	}

	@Test
	public void testPlayerWebRtcVideoOnlyHttpAvi() throws Exception {
		testPlayerWithSmallFileVideoOnly(HTTP, AVI, VIDEO_ONLY);
	}

	@Test
	public void testPlayerWebRtcVideoOnlyHttpMkv() throws Exception {
		testPlayerWithSmallFileVideoOnly(HTTP, MKV, VIDEO_ONLY);
	}

	@Test
	public void testPlayerWebRtcVideoOnlyHttpMov() throws Exception {
		testPlayerWithSmallFileVideoOnly(HTTP, MOV, VIDEO_ONLY);
	}

	@Test
	public void testPlayerWebRtcVideoOnlyHttpMp4() throws Exception {
		testPlayerWithSmallFileVideoOnly(HTTP, MP4, VIDEO_ONLY);
	}

	@Test
	public void testPlayerWebRtcVideoOnlyHttpOgv() throws Exception {
		testPlayerWithSmallFileVideoOnly(HTTP, OGV, VIDEO_ONLY);
	}

	@Test
	public void testPlayerWebRtcVideoOnlyHttpWebm() throws Exception {
		testPlayerWithSmallFileVideoOnly(HTTP, WEBM, VIDEO_ONLY);
	}

	@Test
	public void testPlayerWebRtcVideoOnlyFile3gp() throws Exception {
		testPlayerWithSmallFileVideoOnly(FILE, THIRDGP, VIDEO_ONLY);
	}

	@Test
	public void testPlayerWebRtcVideoOnlyFileAvi() throws Exception {
		testPlayerWithSmallFileVideoOnly(FILE, AVI, VIDEO_ONLY);
	}

	@Test
	public void testPlayerWebRtcVideoOnlyFileMkv() throws Exception {
		testPlayerWithSmallFileVideoOnly(FILE, MKV, VIDEO_ONLY);
	}

	@Test
	public void testPlayerWebRtcVideoOnlyFileMov() throws Exception {
		testPlayerWithSmallFileVideoOnly(FILE, MOV, VIDEO_ONLY);
	}

	@Test
	public void testPlayerWebRtcVideoOnlyFileMp4() throws Exception {
		testPlayerWithSmallFileVideoOnly(FILE, MP4, VIDEO_ONLY);
	}

	@Test
	public void testPlayerWebRtcVideoOnlyFileOgv() throws Exception {
		testPlayerWithSmallFileVideoOnly(FILE, OGV, VIDEO_ONLY);
	}

}
