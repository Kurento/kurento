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
	private long swap;
	private double memPercent;
	private double swapPercent;
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

	public int getNumThreadsKms() {
		return numThreadsKms;
	}

	public void setNumThreadsKms(int numThreadsKms) {
		this.numThreadsKms = numThreadsKms;
	}

	@Override
	public String toString() {
		return "SystemInfo [cpuPercent=" + cpuPercent + ", mem=" + mem
				+ ", swap=" + swap + ", memPercent=" + memPercent
				+ ", swapPercent=" + swapPercent + ", netInfo=" + netInfo
				+ ", numThreadsKms=" + numThreadsKms + "]";
	}

}