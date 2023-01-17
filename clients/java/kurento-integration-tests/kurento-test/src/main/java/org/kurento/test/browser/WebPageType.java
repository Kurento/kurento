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
 * Kind of client (Player, WebRTC, and so on).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public enum WebPageType {
  PLAYER, WEBRTC, ROOM, SCREEN, ROOT, MULTIBROWSER, MULTISESSION;

  private static final String ROOT_VALUE = "/";
  private static final String PLAYER_VALUE = "/player.html";
  private static final String ROOM_VALUE = "/room.html";
  private static final String SCREEN_VALUE = "/index.html";
  private static final String WEBRTC_VALUE = "/webrtc.html";
  private static final String MULTIBROWSER_VALUE = "/multibrowser.html";
  private static final String MULTISESSION_VALUE = "/multisession.html";

  @Override
  public String toString() {
    switch (this) {
      case ROOT:
        return ROOT_VALUE;
      case PLAYER:
        return PLAYER_VALUE;
      case ROOM:
        return ROOM_VALUE;
      case SCREEN:
        return SCREEN_VALUE;
      case MULTIBROWSER:
        return MULTIBROWSER_VALUE;
      case MULTISESSION:
        return MULTISESSION_VALUE;
      case WEBRTC:
      default:
        return WEBRTC_VALUE;
    }
  }

  public static WebPageType value2WebPageType(String value) {
    switch (value) {
      case ROOT_VALUE:
        return ROOT;
      case PLAYER_VALUE:
        return PLAYER;
      case ROOM_VALUE:
        return ROOM;
      case SCREEN_VALUE:
        return SCREEN;
      case MULTIBROWSER_VALUE:
        return MULTIBROWSER;
      case MULTISESSION_VALUE:
        return MULTISESSION;
      case WEBRTC_VALUE:
      default:
        return WEBRTC;
    }
  }

}
