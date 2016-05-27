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

/**
 * Interface of monitor for KMS.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public abstract class KmsMonitor {

  public KmsSystemInfo measureKms() {
    KmsSystemInfo info = new KmsSystemInfo();

    // Bandwidth (bytes tx and rx)
    NetInfo newNetInfo = getNetInfo();
    info.setNetInfo(newNetInfo);

    // CPU usage (%)
    info.setCpuPercent(getCpuUsage());

    // Memory and swap usage (bytes)
    double[] mem = getMem();
    info.setMem((long) mem[0]);
    info.setMemPercent(mem[1]);

    // Number of threads
    info.setNumThreadsKms(getNumThreads());

    return info;
  }

  protected abstract NetInfo getNetInfo();

  protected abstract double getCpuUsage();

  protected abstract double[] getMem();

  protected abstract int getNumThreads();

  protected abstract int getKmsPid();
}
