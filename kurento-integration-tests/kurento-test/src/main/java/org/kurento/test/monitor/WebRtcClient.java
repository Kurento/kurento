
package org.kurento.test.monitor;

import java.util.HashMap;
import java.util.Map;

import org.kurento.client.MediaType;
import org.kurento.client.RTCInboundRTPStreamStats;
import org.kurento.client.RTCOutboundRTPStreamStats;
import org.kurento.client.Stats;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.browser.WebPage;

public class WebRtcClient {

  public static final String INBOUND = "webrtcendpoint_inbound";
  public static final String OUTBOUND = "webrtcendpoint_outbound";

  private String id;
  private WebPage webPage;
  private WebRtcEndpoint webRtcEndpoint;

  public WebRtcClient(String id, WebRtcEndpoint webRtcEndpoint, WebPage webPage) {
    this.id = id;
    this.webPage = webPage;
    this.webRtcEndpoint = webRtcEndpoint;
  }

  public WebPage getWebPage() {
    return webPage;
  }

  public WebRtcEndpoint getWebRtcEndpoint() {
    return webRtcEndpoint;
  }

  @Override
  public String toString() {
    return "WebRtcClient [webPage=" + webPage + ", webRtcEndpoint=" + webRtcEndpoint + "]";
  }

  public WebRtcStats getWebRtcStats() {

    PeerConnectionStats pcStats = null;
    if (webPage != null) {
      pcStats = webPage.getRtcStats();
    }

    WebRtcEndpointStats webRtcEpStats = null;
    if (webRtcEndpoint != null) {
      webRtcEpStats = getStats(webRtcEndpoint);
    }

    return new WebRtcStats(id, pcStats, webRtcEpStats);
  }

  private WebRtcEndpointStats getStats(WebRtcEndpoint webRtcEndpoint) {
    Map<String, Stats> stats = new HashMap<>();
    MediaType[] types = { MediaType.VIDEO, MediaType.AUDIO, MediaType.DATA };

    for (MediaType type : types) {
      Map<String, Stats> trackStats = webRtcEndpoint.getStats(type);
      for (Stats track : trackStats.values()) {
        stats.put(type.name().toLowerCase() + "_" + getRtcStatsType(track.getClass()), track);
      }
    }

    return new WebRtcEndpointStats(stats);
  }

  private String getRtcStatsType(Class<?> clazz) {
    String type = clazz.getSimpleName();
    if (clazz.equals(RTCInboundRTPStreamStats.class)) {
      type = INBOUND;
    } else if (clazz.equals(RTCOutboundRTPStreamStats.class)) {
      type = OUTBOUND;
    }
    return type;
  }
}
