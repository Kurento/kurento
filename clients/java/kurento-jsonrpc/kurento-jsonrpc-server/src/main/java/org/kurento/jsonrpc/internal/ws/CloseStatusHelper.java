/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

package org.kurento.jsonrpc.internal.ws;

public class CloseStatusHelper {

  public static String getCloseStatusType(int code) {

    switch (code) {
      case 1000:
        return "NORMAL";
      case 1001:
        return "GOING_AWAY";
      case 1002:
        return "PROTOCOL_ERROR";
      case 1003:
        return "NOT_ACEPTABLE";
      case 1005:
        return "NO_STATUS_CODE";
      case 1006:
        return "NO_CLOSE_FRAME";
      case 1007:
        return "BAD_DATA";
      case 1008:
        return "POLICY_VIOLATION";
      case 1009:
        return "TOO_BIG_TO_PROCESS";
      case 1010:
        return "REQUIRED_EXTENSION";
      case 1011:
        return "SERVER_ERROR";
      case 1012:
        return "SERVICE_RESTARTED";
      case 1013:
        return "SERVICE_OVERLOAD";
      case 1015:
        return "TLS_HANDSHAKE_FAILURE";
      case 4500:
        return "SESSION_NOT_RELIABLE";
      default:
        return "UNKNOWN";
    }
  }
}
