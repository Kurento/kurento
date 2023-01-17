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
import java.util.Map;
import java.util.TreeMap;

/**
 * Network information (received and sent bytes in the network interfaces of the machine running the
 * tests).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class NetInfo implements Serializable {

  private static final long serialVersionUID = -6318529719103095127L;

  private Map<String, NetInfoEntry> netInfoMap;

  public NetInfo() {
    this.netInfoMap = new TreeMap<>();
  }

  public void putNetInfo(String key, long rxBytes, long txBytes) {
    netInfoMap.put(key, new NetInfoEntry(rxBytes, txBytes));
  }

  public void decrementInitInfo(NetInfo initNetInfo) {
    for (String key : netInfoMap.keySet()) {
      netInfoMap.get(key).decrementRxBytes(initNetInfo.getNetInfoMap().get(key).getRxBytes());
      netInfoMap.get(key).decrementTxBytes(initNetInfo.getNetInfoMap().get(key).getTxBytes());
    }
  }

  public Map<String, NetInfoEntry> getNetInfoMap() {
    return netInfoMap;
  }

  public void setNetInfoMap(Map<String, NetInfoEntry> netInfoMap) {
    this.netInfoMap = netInfoMap;
  }

  public String createHeader() {
    StringBuilder sb = new StringBuilder();
    for (String key : netInfoMap.keySet()) {
      sb.append(",interface_" + key + "_rx_bytes_sum" + ",interface_" + key + "_tx_bytes_sum");
    }
    return sb.toString();
  }

  public String createEntries() {
    StringBuilder sb = new StringBuilder();
    for (String key : netInfoMap.keySet()) {
      sb.append("," + netInfoMap.get(key).getRxBytes() + "," + netInfoMap.get(key).getTxBytes());
    }
    return sb.toString();
  }

  class NetInfoEntry implements Serializable {

    private static final long serialVersionUID = -7279516312913824339L;

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
      return "NetInfoEntry [rxBytes=" + rxBytes + ", txBytes=" + txBytes + "]";
    }

  }

  @Override
  public String toString() {
    return "NetInfo [netInfoMap=" + netInfoMap + "]";
  }

}
