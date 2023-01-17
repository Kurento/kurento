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

package org.kurento.test.browser;

/**
 * WebRTC communication mode (send and receive, send only, or receive only).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.4
 */
public enum WebRtcMode {
  SEND_RCV, SEND_ONLY, RCV_ONLY;

  public String getJsFunction() {
    switch (this) {
      case SEND_RCV:
        return "startSendRecv();";
      case SEND_ONLY:
        return "startSendOnly();";
      case RCV_ONLY:
        return "startRecvOnly();";
      default:
        throw new IllegalArgumentException();
    }
  }

  @Override
  public String toString() {
    switch (this) {
      case SEND_RCV:
        return "(SEND & RECEIVE)";
      case SEND_ONLY:
        return "(SEND ONLY)";
      case RCV_ONLY:
        return "(RECEIVE ONLY)";
      default:
        throw new IllegalArgumentException();
    }
  }
}
