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
 * Type of internet protocol version
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.3.1
 */
public enum WebRtcIpvMode {
  IPV4, IPV6, BOTH;

  @Override
  public String toString() {
    switch (this) {
      case IPV4:
        return "IPV4";
      case IPV6:
        return "IPV6";
      case BOTH:
        return "BOTH";
      default:
        throw new IllegalArgumentException();
    }
  }
}
