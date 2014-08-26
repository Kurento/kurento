/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package org.kurento.test.client;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.CodeFoundEvent;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.ZBarFilter;
import org.kurento.test.base.BrowserKurentoClientTest;

/**
 * <strong>Description</strong>: Test of a HTTP Player with ZBar Filter.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>PlayerEndpoint -> ZBarFilter -> HttpGetEndpoint</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Browser starts before default timeout</li>
 * <li>Browser ends before default timeout</li>
 * <li>CodeFound events received</li>
 * <li>EOS event received</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class PlayerZBarBrowserTest extends BrowserKurentoClientTest {

	@Test
	public void testPlayerZBar() throws Exception {
		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();
		PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
				"http://files.kurento.org/video/barcodes.webm").build();
		HttpGetEndpoint httpEP = new HttpGetEndpoint.Builder(mp)
				.terminateOnEOS().build();
		ZBarFilter zBarFilter = new ZBarFilter.Builder(mp).build();
		playerEP.connect(zBarFilter);
		zBarFilter.connect(httpEP);

		final List<EndOfStreamEvent> eosEvents = new ArrayList<>();
		playerEP.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				eosEvents.add(event);
			}
		});

		final List<CodeFoundEvent> codeFoundEvents = new ArrayList<>();
		zBarFilter.addCodeFoundListener(new EventListener<CodeFoundEvent>() {
			@Override
			public void onEvent(CodeFoundEvent event) {
				log.info("CodeFound {}", event.getValue());
				codeFoundEvents.add(event);
			}
		});

		// Test execution
		try (BrowserClient browser = new BrowserClient.Builder()
				.browser(Browser.CHROME).client(Client.PLAYER).build()) {
			browser.setURL(httpEP.getUrl());
			browser.subscribeEvents("playing", "ended");
			playerEP.play();
			browser.start();

			// Assertions
			Assert.assertTrue("Timeout waiting playing event",
					browser.waitForEvent("playing"));
			Assert.assertTrue("Timeout waiting ended event",
					browser.waitForEvent("ended"));
			Assert.assertTrue("Play time must be at least 12 seconds",
					browser.getCurrentTime() > 12);
			Assert.assertFalse("No code found by ZBar filter",
					codeFoundEvents.isEmpty());
			Assert.assertFalse("No EOS event", eosEvents.isEmpty());
		}
	}

}
