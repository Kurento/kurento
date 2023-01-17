/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

import java.util.ArrayList;
import java.util.List;

public class WebRtcStats {

  private String id;
  private PeerConnectionStats pcStats;
  private WebRtcEndpointStats webRtcEpStats;

  public WebRtcStats(String id, PeerConnectionStats peerConnectionStats,
      WebRtcEndpointStats webRtcEndpointStats) {
    this.id = id;
    this.pcStats = peerConnectionStats;
    this.webRtcEpStats = webRtcEndpointStats;
  }

  public String getId() {
    return id;
  }

  public PeerConnectionStats getPeerConnectionStats() {
    return pcStats;
  }

  public WebRtcEndpointStats getWebRtcEndpointStats() {
    return webRtcEpStats;
  }

  @Override
  public String toString() {
    return "WebRtcStats [id=" + id + ", pcStats=" + pcStats + ", webRtcEpStats=" + webRtcEpStats
        + "]";
  }

  public List<String> calculateHeaders() {

    List<String> headers = new ArrayList<>();

    if (pcStats != null) {
      for (String header : pcStats.calculateHeaders()) {
        headers.add(header);
      }
    }

    if (webRtcEpStats != null) {
      for (String header : webRtcEpStats.calculateHeaders()) {
        headers.add(header);
      }
    }

    return headers;
  }

  public List<Object> calculateValues(List<String> headers) {

    List<Object> values = null;

    if (pcStats != null) {
      values = pcStats.calculateValues(headers);
    }

    if (webRtcEpStats != null) {
      List<Object> webRtcValues = webRtcEpStats.calculateValues(headers);
      if (values == null) {
        values = webRtcValues;
      } else {
        for (int i = 0; i < values.size(); i++) {
          if (values.get(i) == null) {
            values.set(i, webRtcValues.get(i));
          }
        }
      }
    }

    if (values == null) {
      values = new ArrayList<>();
      for (int i = 0; i < headers.size(); i++) {
        values.add(null);
      }
    }

    return values;
  }
}
