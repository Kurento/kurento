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
package org.kurento.test.metatest;

import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.test.base.KurentoClientWebPageTest;
import org.kurento.test.browser.WebRtcTestPage;
import org.kurento.test.config.TestScenario;

/**
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 6.1.1
 */

public class MetaTestMountedVolumeTest
		extends KurentoClientWebPageTest<WebRtcTestPage> {

	public MetaTestMountedVolumeTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return TestScenario.localChrome();
	}

	@Test
	public void test() throws InterruptedException {

		// Media Pipeline
		MediaPipeline mp = kurentoClient.createMediaPipeline();

		String videoPath = "file://" + getPathTestFiles()
				+ "/video/barcodes.webm";

		PlayerEndpoint p = new PlayerEndpoint.Builder(mp, videoPath).build();

		final CountDownLatch latch = new CountDownLatch(1);

		p.addErrorListener(new EventListener<ErrorEvent>() {
			@Override
			public void onEvent(ErrorEvent event) {
				log.warn("Error un player: " + event.getDescription());
				latch.countDown();
			}
		});

		p.play();

		if (latch.await(5, TimeUnit.SECONDS)) {
			fail("Player error");
		}

		// Release Media Pipeline
		mp.release();
	}
}
