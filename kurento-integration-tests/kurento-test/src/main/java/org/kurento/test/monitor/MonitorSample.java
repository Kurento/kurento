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
package org.kurento.test.monitor;

import java.util.HashMap;
import java.util.Map;

/**
 * Monitor results.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class MonitorSample {

	// Number of clients
	private int numClients = 0;

	// KMS information
	private KmsSystemInfo kmsInfo;

	// WebRtcStats
	private Map<String, WebRtcStats> stats = new HashMap<>();

	// Latency
	private double currentLatency = 0;
	private int latencyHints = 0;
	private int latencyErrors = 0;
	private boolean showLantency = false;

	public void addWebRtcStats(WebRtcStats webRtcStats) {
		stats.put(webRtcStats.getId(), webRtcStats);
	}

	public WebRtcStats getWebRtcStats(String id) {
		return stats.get(id);
	}

	public Map<String, WebRtcStats> getStats() {
		return stats;
	}

	public int getNumClients() {
		return numClients;
	}

	public void setNumClients(int numClients) {
		this.numClients = numClients;
	}

	public KmsSystemInfo getSystemInfo() {
		return kmsInfo;
	}

	public void setSystemInfo(KmsSystemInfo kmsInfo) {
		this.kmsInfo = kmsInfo;
	}

	public double getCurrentLatency() {
		return currentLatency;
	}

	public void setCurrentLatency(double currentLatency) {
		this.currentLatency = currentLatency;
	}

	public int getLatencyHints() {
		return latencyHints;
	}

	public void setLatencyHints(int latencyHints) {
		this.latencyHints = latencyHints;
	}

	public int getLatencyErrors() {
		return latencyErrors;
	}

	public void setLatencyErrors(int latencyErrors) {
		this.latencyErrors = latencyErrors;
	}

	public boolean isShowLantency() {
		return showLantency;
	}

	public void setShowLantency(boolean showLantency) {
		this.showLantency = showLantency;
	}

	public double getLatency() {
		double latency = (latencyHints > 0) ? currentLatency / latencyHints : 0;
		this.currentLatency = 0;
		this.latencyHints = 0;
		return latency;
	}

}
