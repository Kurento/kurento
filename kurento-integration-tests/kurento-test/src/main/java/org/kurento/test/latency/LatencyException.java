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

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

/**
 * Latency exception.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class LatencyException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private long latency;
	private TimeUnit latencyTimeUnit;
	private String lastLocalColor;
	private String lastRemoteColor;
	private long lastLocalColorChangeTime;
	private long lastRemoteColorChangeTime;

	public LatencyException(long latency, TimeUnit latencyTimeUnit) {
		this.latency = latency;
		this.latencyTimeUnit = latencyTimeUnit;
	}

	public LatencyException(long latency, TimeUnit latencyTimeUnit,
			String lastLocalColor, String lastRemoteColor,
			long lastLocalColorChangeTime, long lastRemoteColorChangeTime) {
		this(latency, latencyTimeUnit);
		this.lastLocalColor = lastLocalColor;
		this.lastRemoteColor = lastRemoteColor;
		this.lastLocalColorChangeTime = lastLocalColorChangeTime;
		this.lastRemoteColorChangeTime = lastRemoteColorChangeTime;
	}

	@Override
	public String getMessage() {
		String message = "Latency error detected: " + latency + " "
				+ latencyTimeUnit;
		if (lastLocalColor != null) {
			String parsedLocaltime = new SimpleDateFormat("mm:ss.SSS")
					.format(lastLocalColorChangeTime);
			String parsedRemotetime = new SimpleDateFormat("mm:ss.SSS")
					.format(lastRemoteColorChangeTime);
			message += " between last color change in remote tag (color="
					+ lastRemoteColor + " at minute " + parsedRemotetime
					+ ") and last color change in local tag (color="
					+ lastLocalColor + " at minute " + parsedLocaltime + ")";
		}
		return message;
	}

}
