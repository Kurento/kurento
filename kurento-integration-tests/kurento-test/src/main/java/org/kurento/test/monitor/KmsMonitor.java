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
