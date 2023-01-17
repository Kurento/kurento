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

import java.awt.Color;

/**
 * Latency registry (for store and draw latency results compiled for LatencyController).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class LatencyRegistry {

  private Color color;
  private long latency; // millisecons
  private boolean latencyError;
  private LatencyException latencyException;

  public LatencyRegistry() {
    this(null, 0);
  }

  public LatencyRegistry(long latency) {
    this(null, latency);
  }

  public LatencyRegistry(Color color, long latency) {
    this.color = color;
    this.latency = latency;

    // By default, we suppose everything went fine
    this.latencyError = false;
    this.latencyException = null;
  }

  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  public long getLatency() {
    return latency;
  }

  public void setLatency(long latency) {
    this.latency = latency;
  }

  public boolean isLatencyError() {
    return latencyError;
  }

  public void setLatencyError(boolean latencyError) {
    this.latencyError = latencyError;
  }

  public LatencyException getLatencyException() {
    return latencyException;
  }

  public void setLatencyException(LatencyException latencyException) {
    this.latencyError = true;
    this.latencyException = latencyException;
  }

}
