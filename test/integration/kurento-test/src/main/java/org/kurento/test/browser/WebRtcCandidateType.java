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

import org.kurento.test.base.KurentoTest;

/**
 * Type of candidate
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.3.1
 */
public enum WebRtcCandidateType {
  HOST, RELAY, SRFLX, ALL, PRFLX;

  public static WebRtcCandidateType find(String candidateType) {
    for (WebRtcCandidateType v : values()) {
      if (v.toString().equals(candidateType)) {
        return v;
      }
    }
    return null;
  }

  public String getJsFunction() {
    String url;
    String username;
    String password;
    switch (this) {
      case RELAY:
        url = KurentoTest.getTestIceServerUrl();
        username = KurentoTest.getTestIceServerUsername();
        password = KurentoTest.getTestIceServerCredential();
        return "setIceServers('" + url + "', '" + username + "', '" + password + "');";
      case SRFLX:
        url = KurentoTest.getTestStunServerUrl();
        return "setIceServers('" + url + "', '', '');";
      case HOST:
      case ALL:
        return null;
      default:
        throw new IllegalArgumentException();
    }
  }

  @Override
  public String toString() {
    switch (this) {
      case HOST:
        return "host";
      case RELAY:
        return "relay";
      case SRFLX:
        return "srflx";
      case PRFLX:
        return "prflx";
      case ALL:
        return "all";
      default:
        throw new IllegalArgumentException();
    }
  }
}
