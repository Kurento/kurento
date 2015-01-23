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
package org.kurento.test.monitor;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * System information (CPU usage, memory, swap, and network).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class SystemInfo {
	public Logger log = LoggerFactory.getLogger(SystemInfo.class);

	private double cpuPercent;
	private long mem;
	private long swap;
	private double memPercent;
	private double swapPercent;
	private int numClients;
	private double latency;
	private int latencyErrors;
	private NetInfo netInfo;
	private int numThreadsKms;
	private Map<String, Double> rtcStats = new HashMap<>();
	private int avgHints = 0;

	public double getCpuPercent() {
		return cpuPercent;
	}

	public void setCpuPercent(double cpuPercent) {
		this.cpuPercent = cpuPercent;
	}

	public NetInfo getNetInfo() {
		return netInfo;
	}

	public void setNetInfo(NetInfo netInfo) {
		this.netInfo = netInfo;
	}

	public long getMem() {
		return mem;
	}

	public void setMem(long mem) {
		this.mem = mem;
	}

	public long getSwap() {
		return swap;
	}

	public void setSwap(long swap) {
		this.swap = swap;
	}

	public double getMemPercent() {
		return memPercent;
	}

	public void setMemPercent(double memPercent) {
		this.memPercent = memPercent;
	}

	public double getSwapPercent() {
		return swapPercent;
	}

	public void setSwapPercent(double swapPercent) {
		this.swapPercent = swapPercent;
	}

	public int getNumClients() {
		return numClients;
	}

	public void setNumClients(int numClients) {
		this.numClients = numClients;
	}

	public int getLatencyErrors() {
		return latencyErrors;
	}

	public void setLatencyErrors(int latencyErrors) {
		this.latencyErrors = latencyErrors;
	}

	public double getLatency() {
		return latency;
	}

	public void setLatency(double latency) {
		this.latency = latency;
	}

	public int getNumThreadsKms() {
		return numThreadsKms;
	}

	public void setNumThreadsKms(int numThreadsKms) {
		this.numThreadsKms = numThreadsKms;
	}

	public void addRtcStats(Map<String, Object> stats) {
		if (!stats.isEmpty()) {
			avgHints++;
		}
		updateRtcStats(stats);
	}

	public void updateRtcStats(Map<String, Object> stats) {
		// log.info("Updating rtcStats={}", rtcStats);
		for (String key : stats.keySet()) {
			switch (StatsOperation.map().get(key)) {
			case AVG:
			case SUM:
				double value = 0;
				if (rtcStats.containsKey(key)) {
					value = rtcStats.get(key);
				}
				rtcStats.put(key,
						value + Double.parseDouble((String) stats.get(key)));
				break;
			default:
				break;
			}
		}
		// log.info("Done. Now rtcStats={}", rtcStats);
	}

	public Map<String, Double> getRtcStats() {
		Map<String, Double> result = new HashMap<>(rtcStats);

		// Making averages
		for (String key : rtcStats.keySet()) {
			if (StatsOperation.map().get(key) == StatsOperation.AVG) {
				Double sum = rtcStats.get(key);
				Double avg = sum / avgHints;
				// log.info(
				// "Performing average on field {}. "
				// + "Previous value = {} ; avgHints = {} ; New value = {}",
				// key, sum, avgHints, avg);
				result.put(key, avg);
			}
			// log.info("----------------------------");
		}
		return result;
	}

}
