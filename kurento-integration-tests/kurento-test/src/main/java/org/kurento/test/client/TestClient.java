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
package org.kurento.test.client;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.kurento.test.latency.LatencyException;
import org.kurento.test.monitor.SystemMonitorManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic client for tests using Kurento test infrastructure.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.1.0
 */
public class TestClient {

	public static Logger log = LoggerFactory.getLogger(TestClient.class);

	public BrowserClient browserClient;

	public TestClient() {
	}

	public TestClient(TestClient client) {
		this.browserClient = client.browserClient;
	}

	public BrowserClient getBrowserClient() {
		return browserClient;
	}

	public void setBrowserClient(BrowserClient browserClient) {
		this.browserClient = browserClient;
	}

	public void takeScreeshot(String file) throws IOException {
		File scrFile = ((TakesScreenshot) getBrowserClient().getDriver())
				.getScreenshotAs(OutputType.FILE);
		FileUtils.copyFile(scrFile, new File(file));
	}

	/*
	 * setColorCoordinates
	 */
	public void setColorCoordinates(int x, int y) {
		browserClient.executeScript("kurentoTest.setColorCoordinates(" + x
				+ "," + y + ");");
	}

	/*
	 * checkColor
	 */
	public void checkColor(String... videoTags) {
		String tags = "";
		for (String s : videoTags) {
			if (!tags.isEmpty()) {
				tags += ",";
			}
			tags += "'" + s + "'";
		}
		browserClient.executeScript("kurentoTest.checkColor(" + tags + ");");
	}

	/*
	 * similarColorAt
	 */
	public boolean similarColorAt(String videoTag, Color expectedColor, int x,
			int y) {
		setColorCoordinates(x, y);
		return similarColor(videoTag, expectedColor);

	}

	/*
	 * similarColor
	 */
	public boolean similarColor(String videoTag, Color expectedColor) {
		boolean out;
		final long endTimeMillis = System.currentTimeMillis()
				+ (browserClient.getTimeout() * 1000);

		while (true) {
			out = compareColor(videoTag, expectedColor);
			if (out || System.currentTimeMillis() > endTimeMillis) {
				break;
			} else {
				// Polling: wait 200 ms and check again the color
				// Max wait = timeout variable
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					log.trace("InterruptedException in guard condition ({})",
							e.getMessage());
				}
			}
		}
		return out;
	}

	/*
	 * compareColor
	 */
	public boolean compareColor(String videoTag, Color expectedColor) {
		@SuppressWarnings("unchecked")
		List<Long> realColor = (List<Long>) browserClient
				.executeScriptAndWaitOutput("return kurentoTest.colorInfo['"
						+ videoTag + "'].currentColor;");

		long red = realColor.get(0);
		long green = realColor.get(1);
		long blue = realColor.get(2);

		double distance = Math.sqrt((red - expectedColor.getRed())
				* (red - expectedColor.getRed())
				+ (green - expectedColor.getGreen())
				* (green - expectedColor.getGreen())
				+ (blue - expectedColor.getBlue())
				* (blue - expectedColor.getBlue()));

		boolean out = distance <= browserClient.getColorDistance();
		if (!out) {
			log.error(
					"Difference in color comparision. Expected: {}, Real: {} (distance={})",
					expectedColor, realColor, distance);
		}

		return out;
	}

	/*
	 * activateRtcStats
	 */
	public void activateRtcStats(SystemMonitorManager monitor) {
		try {
			browserClient.executeScript("kurentoTest.activateRtcStats();");
			monitor.addJs(browserClient.getJs());
		} catch (WebDriverException we) {
			// If client is not ready to gather rtc statistics, we just log it
			// as warning (it is not an error itself)
			log.warn("Client does not support RTC statistics"
					+ " (function activateRtcStats() is not defined)");
		}
	}

	/*
	 * getLatency
	 */
	@SuppressWarnings("deprecation")
	public long getLatency() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final long[] out = new long[1];
		Thread t = new Thread() {
			public void run() {
				Object latency = browserClient
						.executeScript("return kurentoTest.getLatency();");
				if (latency != null) {
					out[0] = (Long) latency;
				} else {
					out[0] = Long.MIN_VALUE;
				}
				latch.countDown();
			}
		};
		t.start();
		if (!latch.await(browserClient.getTimeout(), TimeUnit.SECONDS)) {
			t.interrupt();
			t.stop();
			throw new LatencyException("Timeout getting latency ("
					+ browserClient.getTimeout() + "  seconds)");
		}
		return out[0];
	}

	/*
	 * checkLatencyUntil
	 */
	public void checkLatencyUntil(SystemMonitorManager monitor,
			long endTimeMillis) throws InterruptedException, IOException {
		while (true) {
			if (System.currentTimeMillis() > endTimeMillis) {
				break;
			}
			Thread.sleep(100);
			try {
				long latency = getLatency();
				if (latency != Long.MIN_VALUE) {
					monitor.addCurrentLatency(latency);
				}
			} catch (LatencyException le) {
				monitor.incrementLatencyErrors();
			}
		}
	}
}
