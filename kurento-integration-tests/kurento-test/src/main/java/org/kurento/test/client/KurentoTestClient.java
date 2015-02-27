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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.latency.LatencyController;
import org.kurento.test.latency.LatencyException;
import org.kurento.test.latency.VideoTag;
import org.kurento.test.latency.VideoTagType;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.kurento.test.services.Recorder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Specific client for tests within kurento-test project. This logic is linked
 * to client page logic (e.g. webrtc.html).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.1.0
 */
public class KurentoTestClient extends TestClient {

	private List<Thread> callbackThreads = new ArrayList<>();
	private Map<String, CountDownLatch> countDownLatchEvents;

	public KurentoTestClient() {
		countDownLatchEvents = new HashMap<>();
	}

	@After
	@SuppressWarnings("deprecation")
	public void teardownKurentoServices() throws Exception {
		for (Thread t : callbackThreads) {
			t.stop();
		}
	}

	/*
	 * subscribeEvents
	 */
	public void subscribeEvents(String eventType) {
		subscribeEventsToVideoTag("video", eventType);
	}

	/*
	 * subscribeLocalEvents
	 */
	public void subscribeLocalEvents(String eventType) {
		subscribeEventsToVideoTag("local", eventType);
	}

	/*
	 * subscribeEventsToVideoTag
	 */
	public void subscribeEventsToVideoTag(final String videoTag,
			final String eventType) {
		CountDownLatch latch = new CountDownLatch(1);
		countDownLatchEvents.put(browserClient.toString() + eventType, latch);
		addEventListener(videoTag, eventType, new BrowserEventListener() {
			@Override
			public void onEvent(String event) {
				consoleLog(ConsoleLogLevel.info, "Event in " + videoTag
						+ " tag: " + event);
				countDownLatchEvents.get(browserClient.toString() + eventType)
						.countDown();
			}
		});
	}

	/*
	 * setColorCoordinates
	 */
	public void setColorCoordinates(int x, int y) {
		browserClient.getDriver().findElement(By.id("x")).clear();
		browserClient.getDriver().findElement(By.id("y")).clear();
		browserClient.getDriver().findElement(By.id("x"))
				.sendKeys(String.valueOf(x));
		browserClient.getDriver().findElement(By.id("y"))
				.sendKeys(String.valueOf(y));
	}

	/*
	 * waitForEvent
	 */
	public boolean waitForEvent(final String eventType)
			throws InterruptedException {
		if (!countDownLatchEvents.containsKey(browserClient.toString()
				+ eventType)) {
			// We cannot wait for an event without previous subscription
			return false;
		}

		boolean result = countDownLatchEvents.get(
				browserClient.toString() + eventType).await(
				browserClient.getTimeout(), TimeUnit.SECONDS);

		// Record local audio when playing event reaches the browser
		if (eventType.equalsIgnoreCase("playing")
				&& browserClient.getRecordAudio() > 0) {
			if (browserClient.getRemoteNode() != null) {
				Recorder.recordRemote(browserClient.getRemoteNode(),
						browserClient.getRecordAudio(),
						browserClient.getAudioSampleRate(),
						browserClient.getAudioChannel());
			} else {
				Recorder.record(browserClient.getRecordAudio(),
						browserClient.getAudioSampleRate(),
						browserClient.getAudioChannel());
			}
		}

		countDownLatchEvents.remove(browserClient.toString() + eventType);
		return result;
	}

	/*
	 * addEventListener
	 */
	@SuppressWarnings("deprecation")
	public void addEventListener(final String videoTag, final String eventType,
			final BrowserEventListener eventListener) {
		Thread t = new Thread() {
			public void run() {
				browserClient.executeScript(videoTag + ".addEventListener('"
						+ eventType + "', videoEvent, false);");
				try {
					(new WebDriverWait(browserClient.getDriver(),
							browserClient.getTimeout()))
							.until(new ExpectedCondition<Boolean>() {
								public Boolean apply(WebDriver d) {
									return d.findElement(By.id("status"))
											.getAttribute("value")
											.equalsIgnoreCase(eventType);
								}
							});
					eventListener.onEvent(eventType);
				} catch (Throwable t) {
					log.error("~~~ Exception in addEventListener {}",
							t.getMessage());
					t.printStackTrace();
					this.interrupt();
					this.stop();
				}
			}
		};
		callbackThreads.add(t);
		t.setDaemon(true);
		t.start();
	}

	/*
	 * start
	 */
	public void start(String videoUrl) {
		browserClient.executeScript("play('" + videoUrl + "', false);");
	}

	/*
	 * stop
	 */
	public void stop() {
		browserClient.executeScript("terminate();");
	}

	/*
	 * consoleLog
	 */
	public void consoleLog(ConsoleLogLevel level, String message) {
		log.info(message);
		browserClient.executeScript("console." + level.toString() + "('"
				+ message + "');");
	}

	/*
	 * getCurrentTime
	 */
	public double getCurrentTime() {
		log.debug("getCurrentTime() called");
		double currentTime = Double.parseDouble(browserClient.getDriver()
				.findElement(By.id("currentTime")).getAttribute("value"));
		log.debug("getCurrentTime() result: {}", currentTime);
		return currentTime;
	}

	/*
	 * setColorCoordinates
	 */
	public boolean similarColorAt(Color expectedColor, int x, int y) {
		boolean out;
		final long endTimeMillis = System.currentTimeMillis()
				+ (browserClient.getTimeout() * 1000);
		setColorCoordinates(x, y);

		while (true) {
			out = compareColor(expectedColor);
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
	 * similarColor
	 */
	public boolean similarColor(Color expectedColor) {
		return similarColorAt(expectedColor, 0, 0);
	}

	/*
	 * compareColor
	 */
	public boolean compareColor(Color expectedColor) {
		String[] realColor = browserClient.getDriver()
				.findElement(By.id("color")).getAttribute("value").split(",");
		int red = Integer.parseInt(realColor[0]);
		int green = Integer.parseInt(realColor[1]);
		int blue = Integer.parseInt(realColor[2]);

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

	// TODO deprecated
	/*
	 * addChangeColorEventListener
	 */
	@Deprecated
	public void addChangeColorEventListener(BrowserClient browserClient,
			VideoTag type, LatencyController cs) {
		cs.addChangeColorEventListener(type, browserClient.getJs(),
				type.getName());
	}

	/*
	 * addChangeColorEventListener
	 */
	@Deprecated
	public void addChangeColorEventListener(BrowserClient browserClient,
			VideoTag type, LatencyController cs, String name) {
		cs.addChangeColorEventListener(type, browserClient.getJs(), name);
	}

	// TODO improve this
	public void activateLatencyControl(BrowserClient browserClient) {
		subscribeEvents("playing");
	}

	/*
	 * getRemoteTime
	 */
	public long getRemoteTime() {
		Object time = browserClient
				.executeScript(VideoTagType.REMOTE.getTime());
		return (time == null) ? 0 : (Long) time;
	}

	/*
	 * checkLatencyUntil
	 */
	public void checkLatencyUntil(long endTimeMillis)
			throws InterruptedException, IOException {
		while (true) {
			if (System.currentTimeMillis() > endTimeMillis) {
				break;
			}
			Thread.sleep(100);
			try {
				long latency = getLatency();
				if (latency != Long.MIN_VALUE) {
					browserClient.getMonitor().addCurrentLatency(latency);
				}
			} catch (LatencyException le) {
				// log.error("$$$ " + le.getMessage());
				browserClient.getMonitor().incrementLatencyErrors();
			}
		}
	}

	/*
	 * readConsole
	 */
	public String readConsole() {
		return browserClient.getDriver().findElement(By.id("console"))
				.getText();
	}

	/*
	 * compare
	 */
	public boolean compare(double i, double j) {
		return Math.abs(j - i) <= browserClient.getThresholdTime();
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
						.executeScript("return getLatency();");
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
	 * initWebRtc
	 */
	@SuppressWarnings("deprecation")
	public void initWebRtc(final WebRtcEndpoint webRtcEndpoint,
			final WebRtcChannel channel, final WebRtcMode mode)
			throws InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);
		Thread t = new Thread() {
			public void run() {
				initWebRtcSdpProcessor(new SdpOfferProcessor() {
					@Override
					public String processSdpOffer(String sdpOffer) {
						return webRtcEndpoint.processOffer(sdpOffer);
					}
				}, channel, mode);
				latch.countDown();
			}
		};
		t.start();
		if (!latch.await(browserClient.getTimeout(), TimeUnit.SECONDS)) {
			t.interrupt();
			t.stop();
		}
	}

	/*
	 * initWebRtcSdpProcessor
	 */
	public void initWebRtcSdpProcessor(SdpOfferProcessor sdpOfferProcessor,
			WebRtcChannel channel, WebRtcMode mode) {

		// Append WebRTC mode (send/receive and audio/video) to identify test
		addTestName(browserClient, KurentoServicesTestHelper.getTestCaseName()
				+ "." + KurentoServicesTestHelper.getTestName());
		appendStringToTitle(browserClient, mode.toString());
		appendStringToTitle(browserClient, channel.toString());

		// Setting custom audio stream (if necessary)
		String audio = browserClient.getAudio();
		if (audio != null) {
			browserClient.executeScript("setCustomAudio('" + audio + "');");
		}

		// Setting MediaConstraints (if necessary)
		String channelJsFunction = channel.getJsFunction();
		if (channelJsFunction != null) {
			browserClient.executeScript(channelJsFunction);
		}

		// Execute JavaScript kurentoUtils.WebRtcPeer
		browserClient.executeScript(mode.getJsFunction());

		// Wait to valid sdpOffer
		(new WebDriverWait(browserClient.getDriver(),
				browserClient.getTimeout()))
				.until(new ExpectedCondition<Boolean>() {
					public Boolean apply(WebDriver d) {
						return browserClient.executeScript("return sdpOffer;") != null;
					}
				});
		String sdpOffer = (String) browserClient
				.executeScript("return sdpOffer;");
		String sdpAnswer = sdpOfferProcessor.processSdpOffer(sdpOffer);

		// Uncomment this line to debug SDP offer and answer
		// log.debug("**** SDP OFFER: {}", sdpOffer);
		// log.debug("**** SDP ANSWER: {}", sdpAnswer);

		// Encoding in Base64 to avoid parsing errors in JavaScript
		sdpAnswer = new String(Base64.encodeBase64(sdpAnswer.getBytes()));

		// Process sdpAnswer
		browserClient.executeScript("processSdpAnswer('" + sdpAnswer + "');");

	}

	/*
	 * addTestName
	 */
	public void addTestName(BrowserClient browserClient, String testName) {
		try {
			browserClient.executeScript("addTestName('" + testName + "');");
		} catch (WebDriverException we) {
			log.warn(we.getMessage());
		}
	}

	/*
	 * appendStringToTitle
	 */
	public void appendStringToTitle(BrowserClient browserClient,
			String webRtcMode) {
		try {
			browserClient.executeScript("appendStringToTitle('" + webRtcMode
					+ "');");
		} catch (WebDriverException we) {
			log.warn(we.getMessage());
		}
	}

	/*
	 * activateRtcStats
	 */
	public static void activateRtcStats(BrowserClient browserClient) {
		try {
			browserClient.executeScript("activateRtcStats();");
		} catch (WebDriverException we) {
			// If client is not ready to gather rtc statistics, we just log it
			// as warning (it is not an error itself)
			log.warn("Client does not support RTC statistics"
					+ " (function activateRtcStats() is not defined)");
		}
	}

	/*
	 * getRtcStats
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getRtcStats(BrowserClient browserClient) {
		Map<String, Object> out = new HashMap<>();
		try {
			out = (Map<String, Object>) browserClient
					.executeScript("return rtcStats;");
		} catch (WebDriverException we) {
			// If client is not ready to gather rtc statistics, we just log it
			// as warning (it is not an error itself)
			log.warn("Client does not support RTC statistics"
					+ " (variable rtcStats is not defined)");
		}
		return out;
	}

}
