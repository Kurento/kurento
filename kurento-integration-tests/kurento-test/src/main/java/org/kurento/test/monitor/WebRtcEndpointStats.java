package org.kurento.test.monitor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.kurento.client.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebRtcEndpointStats extends MonitorStats {

  private static final Logger log = LoggerFactory.getLogger(WebRtcEndpointStats.class);

  private Map<String, Stats> stats;

  public WebRtcEndpointStats(Map<String, Stats> stats) {
    this.stats = stats;
  }

  public Map<String, Stats> getStats() {
    return stats;
  }

  public List<String> calculateHeaders() {

    List<String> headers = new ArrayList<>();

    for (Entry<String, Stats> statEntry : stats.entrySet()) {
      for (Method method : statEntry.getValue().getClass().getMethods()) {
        if (isGetter(method)) {
          headers.add(statEntry.getKey() + "_" + getGetterName(method));
        }
      }
    }

    return headers;
  }

  public List<Object> calculateValues(List<String> headers) {

    Map<String, Object> rtcServerStatsValues = new HashMap<>();

    for (Entry<String, Stats> statEntry : stats.entrySet()) {
      for (Method method : statEntry.getValue().getClass().getMethods()) {
        if (isGetter(method)) {

          Object value = null;
          try {
            value = method.invoke(statEntry.getValue());
          } catch (Exception e) {
            log.error("Exception invoking method", e);
          }

          String keyList = statEntry.getKey() + "_" + getGetterName(method);

          rtcServerStatsValues.put(keyList, value);
        }
      }
    }

    List<Object> values = new ArrayList<>();
    for (String header : headers) {
      values.add(rtcServerStatsValues.get(header));
    }

    return values;
  }
}
