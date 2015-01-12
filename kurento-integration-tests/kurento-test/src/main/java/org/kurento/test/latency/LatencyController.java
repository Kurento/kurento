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
package org.kurento.test.latency;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.kurento.test.base.KurentoTest;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.monitor.SystemMonitorManager;
import org.openqa.selenium.JavascriptExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Latency controller.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class LatencyController implements
		ChangeColorEventListener<ChangeColorEvent> {

	public Logger log = LoggerFactory.getLogger(LatencyController.class);

	private Map<Long, LatencyRegistry> latencyMap;

	private String name;

	private long latencyThreshold;
	private TimeUnit latencyThresholdTimeUnit;

	private long timeout;
	private TimeUnit timeoutTimeUnit;

	private ChangeColorObservable localChangeColor;
	private ChangeColorObservable remoteChangeColor;

	private long lastLocalColorChangeTime = -1;
	private long lastRemoteColorChangeTime = -1;

	private String lastLocalColor;
	private String lastRemoteColor;

	private Thread localColorTrigger;
	private Thread remoteColorTrigger;

	private Semaphore localEventLatch = new Semaphore(0);
	private Semaphore remoteEventLatch = new Semaphore(0);

	private boolean failIfLatencyProblem;
	private boolean local;

	private BrowserClient localBrowser;

	private long latencyRate;

	public LatencyController(String name) {
		this();
		this.name = name;
	}

	public LatencyController() {
		// Defaults
		latencyThreshold = 3000;
		latencyThresholdTimeUnit = TimeUnit.MILLISECONDS;

		timeout = 30;
		timeoutTimeUnit = TimeUnit.SECONDS;

		failIfLatencyProblem = false;
		local = true;

		latencyRate = 100; // milliseconds

		// Latency map (registry)
		latencyMap = new TreeMap<Long, LatencyRegistry>();
	}

	@Override
	public synchronized void onEvent(ChangeColorEvent e) {
		if (e.getVideoTag() == VideoTag.LOCAL) {
			lastLocalColorChangeTime = e.getTime();
			lastLocalColor = e.getColor();
			localEventLatch.release();
		} else if (e.getVideoTag() == VideoTag.REMOTE) {
			lastRemoteColorChangeTime = e.getTime();
			lastRemoteColor = e.getColor();
			remoteEventLatch.release();
		}
	}

	public void checkLatency(long testTime, TimeUnit testTimeUnit,
			SystemMonitorManager monitor) throws InterruptedException,
			IOException {
		checkRemoteLatency(testTime, testTimeUnit, monitor);
	}

	public void checkLatency(long testTime, TimeUnit testTimeUnit)
			throws InterruptedException, IOException {
		if (local) {
			checkLocalLatency(testTime, testTimeUnit);
		} else {
			checkRemoteLatency(testTime, testTimeUnit, null);
		}
	}

	public void checkLocalLatency(final long testTime,
			final TimeUnit testTimeUnit) throws InterruptedException,
			IOException {
		long playTime = TimeUnit.MILLISECONDS.convert(testTime, testTimeUnit);
		long endTimeMillis = System.currentTimeMillis() + playTime;
		while (true) {
			if (System.currentTimeMillis() > endTimeMillis) {
				break;
			}
			Thread.sleep(latencyRate);

			long latency = 0;
			LatencyRegistry latencyRegistry = new LatencyRegistry();
			try {
				latency = localBrowser.getLatency();

			} catch (LatencyException le) {
				latencyRegistry.setLatencyException(le);
				if (failIfLatencyProblem) {
					throw le;
				}
			}

			long latencyTime = localBrowser.getRemoteTime();
			latencyRegistry.setLatency(latency);

			if (latency > getLatencyThreshold(TimeUnit.MILLISECONDS)) {

				String parsedtime = new SimpleDateFormat("mm-ss.SSS")
						.format(latencyTime);
				localBrowser.takeScreeshot(KurentoTest.getDefaultOutputFile("-"
						+ parsedtime + "-error-screenshot.png"));

				LatencyException latencyException = new LatencyException(
						latency, TimeUnit.MILLISECONDS);

				latencyRegistry.setLatencyException(latencyException);
				if (failIfLatencyProblem) {
					throw latencyException;
				}
			}
			latencyMap.put(latencyTime, latencyRegistry);
		}
	}

	public void checkRemoteLatency(final long testTime,
			final TimeUnit testTimeUnit, SystemMonitorManager monitor) {
		String msgName = (name != null) ? "[" + name + "] " : "";

		if (localChangeColor == null || remoteChangeColor == null) {
			throw new RuntimeException(msgName
					+ "Bad setup in latency controller "
					+ " (local and remote tag of browser(s) needed");
		}

		try {
			final Thread waitingThread = Thread.currentThread();

			Thread t = new Thread() {
				public void run() {
					try {
						testTimeUnit.sleep(testTime);
						waitingThread.interrupt();
					} catch (InterruptedException e) {
					}
				}
			};
			t.setDaemon(true);
			t.start();

			boolean firstTime = true;

			while (true) {
				if (!localEventLatch.tryAcquire(timeout, timeoutTimeUnit)) {
					t.interrupt();

					throw new RuntimeException(msgName
							+ "Change color not detected in LOCAL steam after "
							+ timeout + " " + timeoutTimeUnit);
				}

				if (!remoteEventLatch.tryAcquire(timeout, timeoutTimeUnit)) {
					t.interrupt();
					throw new RuntimeException(
							msgName
									+ "Change color not detected in REMOTE steam after "
									+ timeout + " " + timeoutTimeUnit);
				}

				if (firstTime) {
					firstTime = false;
				} else {
					long latencyMilis = lastRemoteColorChangeTime
							- lastLocalColorChangeTime;

					if (monitor != null) {
						monitor.addCurrentLatency(latencyMilis);
					}

					String parsedLocaltime = new SimpleDateFormat("mm:ss.SSS")
							.format(lastLocalColorChangeTime);
					String parsedRemotetime = new SimpleDateFormat("mm:ss.SSS")
							.format(lastRemoteColorChangeTime);

					if (lastLocalColor.equals(lastRemoteColor)) {
						LatencyRegistry LatencyRegistry = new LatencyRegistry(
								rgba2Color(lastRemoteColor), latencyMilis);

						if (latencyMilis > getLatencyThreshold(TimeUnit.MILLISECONDS)) {
							LatencyException latencyException = new LatencyException(
									latencyMilis, testTimeUnit,
									parsedLocaltime, parsedRemotetime,
									testTime, latencyMilis);
							LatencyRegistry
									.setLatencyException(latencyException);
							if (failIfLatencyProblem) {
								t.interrupt();
								throw latencyException;
							} else {
								log.warn(latencyException.getMessage());
							}
							if (monitor != null) {
								monitor.incrementLatencyErrors();
							}
						}

						latencyMap.put(lastRemoteColorChangeTime,
								LatencyRegistry);
					}
				}
			}

		} catch (IOException e) {
			log.debug("Finished LatencyController thread due to IO Exception");
		} catch (InterruptedException e) {
			log.debug("Finished LatencyController thread due to Interrupted Exception");
		}
		localColorTrigger.interrupt();
		remoteColorTrigger.interrupt();
	}

	public void addChangeColorEventListener(VideoTag type,
			JavascriptExecutor js, String name) {
		final long timeoutSeconds = TimeUnit.SECONDS.convert(timeout,
				timeoutTimeUnit);

		if (type == VideoTag.LOCAL) {
			localChangeColor = new ChangeColorObservable();
			localChangeColor.addListener(this);
			localColorTrigger = new Thread(new ColorTrigger(type, js,
					localChangeColor, timeoutSeconds));
			if (name != null) {
				localColorTrigger.setName(name);
			}
			localColorTrigger.start();
		} else {
			remoteChangeColor = new ChangeColorObservable();
			remoteChangeColor.addListener(this);
			remoteColorTrigger = new Thread(new ColorTrigger(type, js,
					remoteChangeColor, timeoutSeconds));
			if (name != null) {
				remoteColorTrigger.setName(name);
			}
			remoteColorTrigger.start();
		}
	}

	public void drawChart(String filename, int width, int height)
			throws IOException {
		ChartWriter chartWriter = new ChartWriter(latencyMap, getName());
		chartWriter.drawChart(filename, width, height);
	}

	public void writeCsv(String csvTitle) throws IOException {
		PrintWriter pw = new PrintWriter(new FileWriter(csvTitle));
		for (long time : latencyMap.keySet()) {
			pw.println(time + "," + latencyMap.get(time).getLatency());
		}
		pw.close();
	}

	public void logLatencyErrorrs() throws IOException {
		log.debug("---------------------------------------------");
		log.debug("LATENCY ERRORS " + getName());
		log.debug("---------------------------------------------");
		int nErrors = 0;
		for (LatencyRegistry registry : latencyMap.values()) {
			if (registry.isLatencyError()) {
				nErrors++;
				log.debug(registry.getLatencyException().getMessage());
			}
		}

		log.debug("{} errors of latency detected (threshold: {} {})", nErrors,
				latencyThreshold, latencyThresholdTimeUnit);
		log.debug("---------------------------------------------");
	}

	public long getLatencyThreshold(TimeUnit timeUnit) {
		return timeUnit.convert(latencyThreshold, latencyThresholdTimeUnit);
	}

	public long getLatencyThreshold() {
		return latencyThreshold;
	}

	public void setLatencyThreshold(long latencyThreshold,
			TimeUnit latencyThresholdTimeUnit) {
		this.latencyThreshold = latencyThreshold;
		this.latencyThresholdTimeUnit = latencyThresholdTimeUnit;
	}

	public TimeUnit getLatencyTimeUnit() {
		return latencyThresholdTimeUnit;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout, TimeUnit timeoutTimeUnit) {
		this.timeout = timeout;
		this.timeoutTimeUnit = timeoutTimeUnit;
	}

	public TimeUnit getTimeoutTimeUnit() {
		return timeoutTimeUnit;
	}

	private Color rgba2Color(String rgba) {
		String[] rgbaArr = rgba.split(",");
		return new Color(Integer.parseInt(rgbaArr[0]),
				Integer.parseInt(rgbaArr[1]), Integer.parseInt(rgbaArr[2]));
	}

	public void setFailIfLatencyProblem(boolean failIfLatencyProblem) {
		this.failIfLatencyProblem = failIfLatencyProblem;
	}

	public String getName() {
		return name != null ? name : "";
	}

	public void activateLocalLatencyAssessmentIn(BrowserClient browser) {
		local = true;
		localBrowser = browser;
		browser.activateLatencyControl();
	}

	public void activateRemoteLatencyAssessmentIn(BrowserClient browser1,
			BrowserClient browser2) {
		local = false;

		addChangeColorEventListener(VideoTag.LOCAL,
				(JavascriptExecutor) browser1.getWebDriver(), getName() + " "
						+ VideoTag.LOCAL);
		addChangeColorEventListener(VideoTag.REMOTE,
				(JavascriptExecutor) browser2.getWebDriver(), getName() + " "
						+ VideoTag.REMOTE);
	}

	public void setLatencyRate(long latencyRate) {
		this.latencyRate = latencyRate;
	}

}
