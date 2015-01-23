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

import java.util.Map;
import java.util.TreeMap;

/**
 * Network information (received and sent bytes in the network interfaces of the
 * machine running the tests).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class NetInfo {

	private Map<String, NetInfoEntry> netInfoMap;

	public NetInfo() {
		this.netInfoMap = new TreeMap<>();
	}

	public void putNetInfo(String key, long rxBytes, long txBytes) {
		netInfoMap.put(key, new NetInfoEntry(rxBytes, txBytes));
	}

	public void decrementInitInfo(NetInfo initNetInfo) {
		for (String key : netInfoMap.keySet()) {
			netInfoMap.get(key).decrementRxBytes(
					initNetInfo.getNetInfoMap().get(key).getRxBytes());
			netInfoMap.get(key).decrementTxBytes(
					initNetInfo.getNetInfoMap().get(key).getTxBytes());
		}
	}

	public Map<String, NetInfoEntry> getNetInfoMap() {
		return netInfoMap;
	}

	public void setNetInfoMap(Map<String, NetInfoEntry> netInfoMap) {
		this.netInfoMap = netInfoMap;
	}

	public String parseHeaderEntry() {
		String out = "";
		for (String key : netInfoMap.keySet()) {
			out += ", interface_" + key + "_rx_bytes_sum" + " , interface_"
					+ key + "_tx_bytes_sum";
		}
		return out;
	}

	public String parseNetEntry() {
		String out = "";
		for (String key : netInfoMap.keySet()) {
			out += ", " + netInfoMap.get(key).getRxBytes() + ", "
					+ netInfoMap.get(key).getTxBytes();
		}
		return out;
	}

	private class NetInfoEntry {
		private long rxBytes;
		private long txBytes;

		public NetInfoEntry(long rxBytes, long txBytes) {
			this.rxBytes = rxBytes;
			this.txBytes = txBytes;
		}

		public long getRxBytes() {
			return rxBytes;
		}

		public void decrementRxBytes(long rxBytes) {
			this.rxBytes = this.rxBytes - rxBytes;
		}

		public long getTxBytes() {
			return txBytes;
		}

		public void decrementTxBytes(long txBytes) {
			this.txBytes = this.txBytes - txBytes;
		}

		@Override
		public String toString() {
			return "NetInfoEntry [rxBytes=" + rxBytes + ", txBytes=" + txBytes
					+ "]";
		}

	}

	@Override
	public String toString() {
		return "NetInfo [netInfoMap=" + netInfoMap + "]";
	}

}
