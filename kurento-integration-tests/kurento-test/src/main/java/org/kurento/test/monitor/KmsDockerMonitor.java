/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.test.monitor;

import java.util.List;
import java.util.Map;

import org.kurento.test.docker.Docker;

import com.github.dockerjava.api.model.CpuStatsConfig;
import com.github.dockerjava.api.model.MemoryStatsConfig;
import com.github.dockerjava.api.model.StatisticNetworksConfig;
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

  private long previousCpu = -1;
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
    Map<String, StatisticNetworksConfig> networksStats = stats.getNetworks();

    for (String key : networksStats.keySet()) {
      Map<String, Object> iface = (Map<String, Object>) networksStats.get(key);
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
    CpuStatsConfig cpuStats = stats.getCpuStats();

    if (cpuStats != null) {
      Map<String, Object> cpuUsageMap = (Map<String, Object>) cpuStats.getCpuUsage();
      long systemUsage = Long.parseLong(cpuStats.getSystemCpuUsage().toString());
      long totalUsage = Long.parseLong(cpuUsageMap.get("total_usage").toString());

      List<Object> perCpuUsage = (List<Object>) cpuUsageMap.get("percpu_usage");

      if (previousCpu != -1 && previousSystem != -1) {
        // Using same formula than
        // https://github.com/docker/docker/blob/master/api/client/stats.go

        float cpuDelta = totalUsage - previousCpu;
        float systemDelta = systemUsage - previousSystem;

        if (cpuDelta > 0 && systemDelta > 0) {
          cpuUsage = cpuDelta / systemDelta * perCpuUsage.size() * 100;
        }
      }

      previousCpu = totalUsage;
      previousSystem = systemUsage;
    }

    return cpuUsage;
  }

  @Override
  protected double[] getMem() {
    double[] out = { 0, 0 };

    Statistics stats = docker.getStatistics(containerId);
    MemoryStatsConfig memoryStats = stats.getMemoryStats();
    long usage = memoryStats.getUsage();
    long limit = memoryStats.getLimit();
    double memPercent = usage / limit * 100;

    out[0] = usage;
    out[1] = memPercent;

    return out;
  }

  @Override
  protected int getKmsPid() {
    int kmdPid = -1;
    String execOutput = docker.execCommand(containerId, true, "ps", "axf");

    String[] lines = execOutput.split("\n");
    for (String line : lines) {
      if (line.contains("/usr/bin/kurento-media-server")) {
        kmdPid = Integer.parseInt(line.trim().substring(0, line.trim().indexOf(" ")));
        break;
      }
    }

    return kmdPid;
  }

  @Override
  protected int getNumThreads() {
    int numThreads = -1;
    String kmsStat = docker.execCommand(containerId, true, "cat", "/proc/" + kmsPid + "/stat");
    String[] kmsStats = kmsStat.split(" ");
    if (kmsStats.length >= 20) {
      numThreads = Integer.parseInt(kmsStats[19]);
    }
    return numThreads;
  }

}
