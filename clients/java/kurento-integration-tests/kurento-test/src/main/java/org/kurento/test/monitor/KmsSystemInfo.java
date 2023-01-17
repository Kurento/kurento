/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

import java.io.Serializable;

/**
 * System information (CPU usage, memory, swap, and network).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class KmsSystemInfo implements Serializable {

  private static final long serialVersionUID = -8862615359741666215L;

  private double cpuPercent;
  private long mem;
  private double memPercent;
  private NetInfo netInfo;
  private int numThreadsKms;

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

  public double getMemPercent() {
    return memPercent;
  }

  public void setMemPercent(double memPercent) {
    this.memPercent = memPercent;
  }

  public int getNumThreadsKms() {
    return numThreadsKms;
  }

  public void setNumThreadsKms(int numThreadsKms) {
    this.numThreadsKms = numThreadsKms;
  }

  @Override
  public String toString() {
    return "KmsSystemInfo [cpuPercent=" + cpuPercent + ", mem=" + mem + ", memPercent=" + memPercent
        + ", netInfo=" + netInfo + ", numThreadsKms=" + numThreadsKms + "]";
  }

}
