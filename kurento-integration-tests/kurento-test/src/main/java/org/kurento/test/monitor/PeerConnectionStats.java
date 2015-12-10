package org.kurento.test.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PeerConnectionStats extends MonitorStats {

  private Map<String, Object> stats;

  public PeerConnectionStats(Map<String, Object> stats) {
    this.stats = stats;
  }

  public Map<String, Object> getStats() {
    return stats;
  }

  public List<String> calculateHeaders() {
    return new ArrayList<>(stats.keySet());
  }

  public List<Object> calculateValues(List<String> headers) {

    List<Object> values = new ArrayList<>();
    for (String header : headers) {
      values.add(stats.get(header));
    }
    return values;
  }

}
