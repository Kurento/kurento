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

/**
 * Video tag (local, remote).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public enum VideoTagType {
  LOCAL, REMOTE;

  public static String localId;
  public static String remoteId;

  public String getColor() {
    switch (this) {
      case LOCAL:
        return "return kurentoTest.colorInfo['" + localId + "'].changeColor;";
      case REMOTE:
      default:
        return "return kurentoTest.colorInfo['" + remoteId + "'].changeColor;";
    }
  }

  public String getTime() {
    switch (this) {
      case LOCAL:
        return "return kurentoTest.colorInfo['" + localId + "'].changeTime;";
      case REMOTE:
      default:
        return "return kurentoTest.colorInfo['" + remoteId + "'].changeTime;";
    }
  }

  @Override
  public String toString() {
    switch (this) {
      case LOCAL:
        return "local stream";
      case REMOTE:
      default:
        return "remote stream";
    }
  }

  public String getName() {
    switch (this) {
      case LOCAL:
        return "local";
      case REMOTE:
      default:
        return "remote";
    }
  }

  public String getId() {
    if (localId == null || remoteId == null) {
      throw new RuntimeException(
          "You must specify local/remote video tag id in order to perform latency control");
    }

    switch (this) {
      case LOCAL:
        return localId;
      case REMOTE:
      default:
        return remoteId;
    }
  }

  public static void setLocalId(String id) {
    localId = id;
  }

  public static void setRemoteId(String id) {
    remoteId = id;
  }

}
