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
package org.kurento.test.base;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.testing.IntegrationTests;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserEventListener;
import org.kurento.test.client.BrowserType;
import org.kurento.test.client.Client;
import org.kurento.test.client.ConsoleLogLevel;
import org.kurento.test.client.SdpOfferProcessor;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.Protocol;
import org.kurento.test.config.TestScenario;
import org.kurento.test.latency.LatencyController;
import org.kurento.test.latency.LatencyException;
import org.kurento.test.latency.VideoTag;
import org.kurento.test.latency.VideoTagType;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.kurento.test.services.Recorder;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Base for tests using kurento-client and HTTP Server.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
@EnableAutoConfiguration
@Category(IntegrationTests.class)
public class BrowserKurentoClientTest extends KurentoClientTest {

	public static final Color CHROME_VIDEOTEST_COLOR = new Color(0, 135, 0);
	private static final String SDP_DELIMITER = "\r\n";

	private List<Thread> callbackThreads = new ArrayList<>();
	private Map<String, CountDownLatch> countDownLatchEvents;

	public BrowserKurentoClientTest(TestScenario testScenario) {
		super(testScenario);
		countDownLatchEvents = new HashMap<>();
	}

	public BrowserKurentoClientTest() {
		super();
	}

	@Rule
	public KmsLogOnFailure logOnFailure = new KmsLogOnFailure();

	@After
	@SuppressWarnings("deprecation")
	public void teardownKurentoServices() throws Exception {
		for (Thread t : callbackThreads) {
			t.stop();
		}
	}

	/*
	 * If not specified, the default file for recording will have ".webm"
	 * extension.
	 */
	public String getDefaultFileForRecording() {
		return getDefaultOutputFile(".webm");
	}

	public static String getDefaultOutputFile(String preffix) {
		File fileForRecording = new File(KurentoServicesTestHelper.getTestDir()
				+ "/" + KurentoServicesTestHelper.getTestCaseName());
		String testName = KurentoServicesTestHelper.getSimpleTestName();
		return fileForRecording.getAbsolutePath() + "/" + testName + preffix;
	}

	protected void playFileWithPipeline(BrowserType browserType,
			String recordingFile, int playtime, int x, int y,
			Color... expectedColors) throws InterruptedException {

		MediaPipeline mp = null;
		try (BrowserClient browserClient = new BrowserClient.Builder()
				.browserType(browserType).client(Client.WEBRTC).build()) {
			// Media Pipeline
			mp = kurentoClient.createMediaPipeline();
			PlayerEndpoint playerEP = new PlayerEndpoint.Builder(mp,
					recordingFile).build();
			WebRtcEndpoint webRtcEP = new WebRtcEndpoint.Builder(mp).build();
			playerEP.connect(webRtcEP);

			// Play latch
			final CountDownLatch eosLatch = new CountDownLatch(1);
			playerEP.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
				@Override
				public void onEvent(EndOfStreamEvent event) {
					eosLatch.countDown();
				}
			});

			// Test execution
			subscribeEvents(browserClient, "playing");
			initWebRtc(browserClient, webRtcEP, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.RCV_ONLY);
			playerEP.play();

			// Assertions
			makeAssertions(browserClient, "[played file with media pipeline]",
					browserClient, playtime, x, y, eosLatch, expectedColors);

		} finally {
			// Release Media Pipeline
			if (mp != null) {
				mp.release();
			}
		}
	}

	protected void playFileAsLocal(BrowserType browserType,
			String recordingFile, int playtime, int x, int y,
			Color... expectedColors) throws InterruptedException {
		try (BrowserClient browserClient = new BrowserClient.Builder()
				.browserType(browserType).client(Client.WEBRTC)
				.protocol(Protocol.FILE).build()) {
			subscribeEvents(browserClient, "playing");
			playUrlInVideoTag(browserClient, recordingFile, VideoTagType.REMOTE);

			// Assertions
			makeAssertions(browserClient, "[played as local file]",
					browserClient, playtime, x, y, null, expectedColors);
		}
	}

	protected void playFileAsLocal(BrowserType browserType,
			String recordingFile, int playtime, Color... expectedColors)
			throws InterruptedException {
		playFileAsLocal(browserType, recordingFile, playtime, 0, 0,
				expectedColors);
	}

	protected void playFileWithPipeline(BrowserType browserType,
			String recordingFile, int playtime, Color... expectedColors)
			throws InterruptedException {
		playFileWithPipeline(browserType, recordingFile, playtime, 0, 0,
				expectedColors);
	}

	private void makeAssertions(BrowserClient browserClient,
			String messageAppend, BrowserClient browser, int playtime, int x,
			int y, CountDownLatch eosLatch, Color... expectedColors)
			throws InterruptedException {
		Assert.assertTrue(
				"Not received media in the recording (timeout waiting playing event) "
						+ messageAppend, waitForEvent(browserClient, "playing"));
		for (Color color : expectedColors) {
			Assert.assertTrue("The color of the recorded video should be "
					+ color + " " + messageAppend,
					similarColorAt(browserClient, color, x, y));
		}

		if (eosLatch != null) {
			Assert.assertTrue("Not received EOS event in player", eosLatch
					.await(browserClient.getTimeout(), TimeUnit.SECONDS));
		} else {
			Thread.sleep(playtime * 1000);
		}

		double currentTime = getCurrentTime(browserClient);
		Assert.assertTrue(
				"Error in play time in the recorded video (expected: "
						+ playtime + " sec, real: " + currentTime + " sec) "
						+ messageAppend,
				compare(browserClient, playtime, currentTime));
	}

	protected String mangleSdp(String sdpIn, String[] removeCodes) {
		String sdpMangled1 = "";
		List<String> indexList = new ArrayList<>();
		for (String line : sdpIn.split(SDP_DELIMITER)) {
			boolean codecFound = false;
			for (String codec : removeCodes) {
				codecFound |= line.contains(codec);
			}
			if (codecFound) {
				String index = line.substring(line.indexOf(":") + 1,
						line.indexOf(" ") + 1);
				indexList.add(index);
			} else {
				sdpMangled1 += line + SDP_DELIMITER;
			}
		}

		String sdpMangled2 = "";
		log.info("indexList " + indexList);
		for (String line : sdpMangled1.split(SDP_DELIMITER)) {
			for (String index : indexList) {
				line = line.replaceAll(index, "");
			}
			sdpMangled2 += line + SDP_DELIMITER;
		}
		return sdpMangled2;
	}

	/*
	 * subscribeEvents
	 */
	public void subscribeEvents(String browserKey, String eventType) {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		subscribeEvents(browserClient, eventType);
	}

	public void subscribeEvents(BrowserClient browserClient, String eventType) {
		subscribeEventsToVideoTag(browserClient, "video", eventType);
	}

	/*
	 * subscribeLocalEvents
	 */
	public void subscribeLocalEvents(String browserKey, String eventType) {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		subscribeEventsToVideoTag(browserClient, "local", eventType);
	}

	public void subscribeLocalEvents(BrowserClient browserClient,
			String eventType) {
		subscribeEventsToVideoTag(browserClient, "local", eventType);
	}

	public void subscribeEventsToVideoTag(final BrowserClient browserClient,
			final String videoTag, final String eventType) {
		CountDownLatch latch = new CountDownLatch(1);
		countDownLatchEvents.put(browserClient.toString() + eventType, latch);
		addEventListener(browserClient, videoTag, eventType,
				new BrowserEventListener() {
					@Override
					public void onEvent(String event) {
						consoleLog(browserClient, ConsoleLogLevel.info,
								"Event in " + videoTag + " tag: " + event);
						countDownLatchEvents.get(
								browserClient.toString() + eventType)
								.countDown();
					}
				});
	}

	/*
	 * setColorCoordinates
	 */
	public void setColorCoordinates(String browserKey, int x, int y) {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		setColorCoordinates(browserClient, x, y);
	}

	public void setColorCoordinates(BrowserClient browserClient, int x, int y) {
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
	public boolean waitForEvent(String browserKey, String eventType)
			throws InterruptedException {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		return waitForEvent(browserClient, eventType);
	}

	public boolean waitForEvent(BrowserClient browserClient,
			final String eventType) throws InterruptedException {
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
	public void addEventListener(String browserKey, String videoTag,
			String eventType, BrowserEventListener eventListener) {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		addEventListener(browserClient, videoTag, eventType, eventListener);
	}

	@SuppressWarnings("deprecation")
	public void addEventListener(final BrowserClient browserClient,
			final String videoTag, final String eventType,
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
	public void start(String browserKey, String videoUrl) {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		start(browserClient, videoUrl);
	}

	public void start(BrowserClient browserClient, String videoUrl) {
		browserClient.executeScript("play('" + videoUrl + "', false);");
	}

	/*
	 * stop
	 */
	public void stop(String browserKey) {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		stop(browserClient);
	}

	public void stop(BrowserClient browserClient) {
		browserClient.executeScript("terminate();");
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
	 * consoleLog
	 */
	public void consoleLog(String browserKey, ConsoleLogLevel level,
			String message) {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		consoleLog(browserClient, level, message);
	}

	public void consoleLog(BrowserClient browserClient, ConsoleLogLevel level,
			String message) {
		log.info(message);
		browserClient.executeScript("console." + level.toString() + "('"
				+ message + "');");
	}

	/*
	 * getCurrentTime
	 */
	public double getCurrentTime(String browserKey) {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		return getCurrentTime(browserClient);
	}

	public double getCurrentTime(BrowserClient browserClient) {
		log.debug("getCurrentTime() called");
		double currentTime = Double.parseDouble(browserClient.getDriver()
				.findElement(By.id("currentTime")).getAttribute("value"));
		log.debug("getCurrentTime() result: {}", currentTime);
		return currentTime;
	}

	/*
	 * setColorCoordinates
	 */
	public boolean similarColorAt(String browserKey, Color expectedColor,
			int x, int y) {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		return similarColorAt(browserClient, expectedColor, x, y);
	}

	public boolean similarColorAt(BrowserClient browserClient,
			Color expectedColor, int x, int y) {
		boolean out;
		final long endTimeMillis = System.currentTimeMillis()
				+ (browserClient.getTimeout() * 1000);
		setColorCoordinates(browserClient, x, y);

		while (true) {
			out = compareColor(browserClient, expectedColor);
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
	public boolean similarColor(String browserKey, Color expectedColor) {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		return similarColorAt(browserClient, expectedColor, 0, 0);
	}

	public boolean similarColor(BrowserClient browserClient, Color expectedColor) {
		return similarColorAt(browserClient, expectedColor, 0, 0);
	}

	/*
	 * compareColor
	 */
	public boolean compareColor(String browserKey, Color expectedColor) {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		return compareColor(browserClient, expectedColor);
	}

	public boolean compareColor(BrowserClient browserClient, Color expectedColor) {
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

	/*
	 * initWebRtcSdpProcessor
	 */
	public void initWebRtcSdpProcessor(String browserKey,
			SdpOfferProcessor sdpOfferProcessor, WebRtcChannel channel,
			WebRtcMode mode) {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		initWebRtcSdpProcessor(browserClient, sdpOfferProcessor, channel, mode);
	}

	public void initWebRtcSdpProcessor(final BrowserClient browserClient,
			SdpOfferProcessor sdpOfferProcessor, WebRtcChannel channel,
			WebRtcMode mode) {

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
	 * initWebRtc
	 */
	public void initWebRtc(String browserKey, WebRtcEndpoint webRtcEndpoint,
			WebRtcChannel channel, WebRtcMode mode) throws InterruptedException {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		initWebRtc(browserClient, webRtcEndpoint, channel, mode);
	}

	@SuppressWarnings("deprecation")
	public void initWebRtc(final BrowserClient browserClient,
			final WebRtcEndpoint webRtcEndpoint, final WebRtcChannel channel,
			final WebRtcMode mode) throws InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);
		Thread t = new Thread() {
			public void run() {
				initWebRtcSdpProcessor(browserClient, new SdpOfferProcessor() {
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

	public void playUrlInVideoTag(BrowserClient browserClient, String url,
			VideoTagType videoTagType) {
		browserClient.executeScript("document.getElementById('"
				+ videoTagType.getId() + "').setAttribute('src', '" + url
				+ "');");
		browserClient.executeScript("document.getElementById('"
				+ videoTagType.getId() + "').load();");
	}

	@Deprecated
	public void addChangeColorEventListener(BrowserClient browserClient,
			VideoTag type, LatencyController cs) {
		cs.addChangeColorEventListener(type, browserClient.getJs(),
				type.getName());
	}

	@Deprecated
	public void addChangeColorEventListener(BrowserClient browserClient,
			VideoTag type, LatencyController cs, String name) {
		cs.addChangeColorEventListener(type, browserClient.getJs(), name);
	}

	/*
	 * takeScreeshot
	 */
	public void takeScreeshot(String browserKey, String file)
			throws IOException {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		takeScreeshot(browserClient, file);
	}

	public static void takeScreeshot(BrowserClient browserClient, String file)
			throws IOException {
		File scrFile = ((TakesScreenshot) browserClient.getDriver())
				.getScreenshotAs(OutputType.FILE);
		FileUtils.copyFile(scrFile, new File(file));
	}

	public void activateLatencyControl(BrowserClient browserClient) {
		this.subscribeEvents(browserClient, "playing");
	}

	public static long getRemoteTime(BrowserClient browserClient) {
		Object time = browserClient
				.executeScript(VideoTagType.REMOTE.getTime());
		return (time == null) ? 0 : (Long) time;
	}

	/*
	 * checkLatencyUntil
	 */
	public void checkLatencyUntil(String browserKey, long endTimeMillis)
			throws InterruptedException, IOException {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		checkLatencyUntil(browserClient, endTimeMillis);
	}

	public void checkLatencyUntil(BrowserClient browserClient,
			long endTimeMillis) throws InterruptedException, IOException {
		while (true) {
			if (System.currentTimeMillis() > endTimeMillis) {
				break;
			}
			Thread.sleep(100);
			try {
				long latency = getLatency(browserClient);
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
	 * checkRemoteLatency
	 */
	public void checkRemoteLatency(String browserKey, long endTimeMillis,
			BrowserClient remoteBrowser) throws InterruptedException,
			IOException {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		checkRemoteLatency(browserClient, endTimeMillis, remoteBrowser);
	}

	public void checkRemoteLatency(BrowserClient browserClient,
			long endTimeMillis, BrowserClient remoteBrowser)
			throws InterruptedException, IOException {

		LatencyController cs = new LatencyController();
		cs.activateRemoteLatencyAssessmentIn(browserClient, remoteBrowser);
		cs.checkLatency(endTimeMillis, TimeUnit.MILLISECONDS,
				browserClient.getMonitor());

	}

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

	public String readConsole(BrowserClient browserClient) {
		return browserClient.getDriver().findElement(By.id("console"))
				.getText();
	}

	/*
	 * compare
	 */
	public boolean compare(String browserKey, double i, double j) {
		BrowserClient browserClient = testScenario.getBrowserMap().get(
				browserKey);
		return compare(browserClient, i, j);
	}

	public boolean compare(BrowserClient browserClient, double i, double j) {
		return Math.abs(j - i) <= browserClient.getThresholdTime();
	}

	@SuppressWarnings("deprecation")
	public static long getLatency(final BrowserClient browserClient)
			throws InterruptedException {
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

}
