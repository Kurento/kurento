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
package org.kurento.test.color;

import java.text.SimpleDateFormat;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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

	private String name;

	private long latency;
	private TimeUnit latencyTimeUnit;

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

	public LatencyController(String name) {
		this();
		this.name = name;
	}

	public LatencyController() {
		// Defaults
		latency = 3000;
		latencyTimeUnit = TimeUnit.MILLISECONDS;

		timeout = 30;
		timeoutTimeUnit = TimeUnit.SECONDS;
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

	public void checkLatency(final long testTime, final TimeUnit testTimeUnit) {
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

					String parsedLocaltime = new SimpleDateFormat("mm:ss.SSS")
							.format(lastLocalColorChangeTime);
					String parsedRemotetime = new SimpleDateFormat("mm:ss.SSS")
							.format(lastRemoteColorChangeTime);

					if (latencyMilis > getLatency(TimeUnit.MILLISECONDS)
							&& lastLocalColor.equals(lastRemoteColor)) {

						t.interrupt();

						throw new RuntimeException(
								msgName
										+ "Latency error detected: "
										+ latencyMilis
										+ " "
										+ latencyTimeUnit
										+ " between last color change in remote tag (color="
										+ lastRemoteColor
										+ " at minute "
										+ parsedRemotetime
										+ ") and last color change in local tag (color="
										+ lastLocalColor + " at minute "
										+ parsedLocaltime + ")");

					}
				}
			}

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

	public long getLatency(TimeUnit timeUnit) {
		return timeUnit.convert(latency, latencyTimeUnit);
	}

	public long getLatency() {
		return latency;
	}

	public void setLatency(long latency, TimeUnit latencyTimeUnit) {
		this.latency = latency;
		this.latencyTimeUnit = latencyTimeUnit;
	}

	public TimeUnit getLatencyTimeUnit() {
		return latencyTimeUnit;
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

}
