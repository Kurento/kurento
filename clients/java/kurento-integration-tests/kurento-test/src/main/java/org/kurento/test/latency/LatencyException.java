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

package org.kurento.test.latency;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

/**
 * Latency exception.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class LatencyException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private long latency;
  private TimeUnit latencyTimeUnit;
  private String lastLocalColor;
  private String lastRemoteColor;
  private long lastLocalColorChangeTime;
  private long lastRemoteColorChangeTime;
  private String message;

  public LatencyException(String message) {
    this.message = message;
  }

  public LatencyException(long latency, TimeUnit latencyTimeUnit) {
    this.latency = latency;
    this.latencyTimeUnit = latencyTimeUnit;
  }

  public LatencyException(long latency, TimeUnit latencyTimeUnit, String lastLocalColor,
      String lastRemoteColor, long lastLocalColorChangeTime, long lastRemoteColorChangeTime) {
    this(latency, latencyTimeUnit);
    this.lastLocalColor = lastLocalColor;
    this.lastRemoteColor = lastRemoteColor;
    this.lastLocalColorChangeTime = lastLocalColorChangeTime;
    this.lastRemoteColorChangeTime = lastRemoteColorChangeTime;
  }

  @Override
  public String getMessage() {
    String out;
    if (message != null) {
      out = message;
    } else {
      out = "Latency error detected: " + latency + " " + latencyTimeUnit;
      if (lastLocalColor != null) {
        String parsedLocaltime = new SimpleDateFormat("mm:ss.SSS").format(lastLocalColorChangeTime);
        String parsedRemotetime =
            new SimpleDateFormat("mm:ss.SSS").format(lastRemoteColorChangeTime);
        out += " between last color change in remote tag (color=" + lastRemoteColor + " at minute "
            + parsedRemotetime + ") and last color change in local tag (color=" + lastLocalColor
            + " at minute " + parsedLocaltime + ")";
      }
    }
    return out;
  }

}
