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

import java.util.List;
import java.util.Map;

import org.kurento.test.docker.Docker;

import com.github.dockerjava.api.model.Statistics;

/**
 * Monitor when KMS is dockerized.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class KmsDockerMonitor extends KmsMonitor {

	private int kmsPid;

	private Docker docker;
	private String containerId;

	private long previousCPU = -1;
	private long previousSystem = -1;

	public KmsDockerMonitor(String containerId) {
		this.containerId = containerId;
		this.docker = Docker.getSingleton();
		this.kmsPid = getKmsPid();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected NetInfo getNetInfo() {
		NetInfo netInfo = new NetInfo();
		Statistics stats = docker.getStatistics(containerId);
		Map<String, Object> networksStats = stats.getNetworks();

		for (String key : networksStats.keySet()) {
			Map<String, Object> iface = (Map<String, Object>) networksStats
					.get(key);
			int rxBytes = (Integer) iface.get("rx_bytes");
			int txBytes = (Integer) iface.get("tx_bytes");

			netInfo.putNetInfo(key, rxBytes, txBytes);
		}
		return netInfo;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected double getCpuUsage() {
		double cpuUsage = 0;
		Statistics stats = docker.getStatistics(containerId);
		Map<String, Object> cpuStats = stats.getCpuStats();

		if (cpuStats != null) {
			Map<String, Object> cpuUsageMap = (Map<String, Object>) cpuStats
					.get("cpu_usage");
			long systemUsage = Long
					.parseLong(cpuStats.get("system_cpu_usage").toString());
			long totalUsage = Long
					.parseLong(cpuUsageMap.get("total_usage").toString());

			List<Object> perCpuUsage = (List<Object>) cpuUsageMap
					.get("percpu_usage");

			if (previousCPU != -1 && previousSystem != -1) {
				// Using same formula than
				// https://github.com/docker/docker/blob/master/api/client/stats.go

				float cpuDelta = totalUsage - previousCPU;
				float systemDelta = systemUsage - previousSystem;

				if (cpuDelta > 0 && systemDelta > 0) {
					cpuUsage = (cpuDelta / systemDelta) * perCpuUsage.size()
							* 100;
				}
			}

			previousCPU = totalUsage;
			previousSystem = systemUsage;
		}

		return cpuUsage;
	}

	@Override
	protected double[] getMem() {
		double[] out = { 0, 0 };

		Statistics stats = docker.getStatistics(containerId);
		Map<String, Object> memoryStats = stats.getMemoryStats();
		int usage = (Integer) memoryStats.get("usage");
		float limit = (Long) memoryStats.get("limit");
		double memPercent = (usage / limit) * 100;

		out[0] = usage;
		out[1] = memPercent;

		return out;
	}

	@Override
	protected int getKmsPid() {
		int kmdPid = -1;
		String execOutput = docker.execCommand(containerId, "ps", "axf");

		String[] lines = execOutput.split("\n");
		for (String line : lines) {
			if (line.contains("/usr/bin/kurento-media-server")) {
				kmdPid = Integer.parseInt(
						line.trim().substring(0, line.trim().indexOf(" ")));
				break;
			}
		}

		return kmdPid;
	}

	@Override
	protected int getNumThreads() {
		int numThreads = -1;
		String kmsStat = docker.execCommand(containerId, "cat",
				"/proc/" + kmsPid + "/stat");
		String[] kmsStats = kmsStat.split(" ");
		if (kmsStats.length >= 20) {
			numThreads = Integer.parseInt(kmsStats[19]);
		}
		return numThreads;
	}

}
