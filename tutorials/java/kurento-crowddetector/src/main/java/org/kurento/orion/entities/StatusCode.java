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

package org.kurento.orion.entities;

import com.google.gson.annotations.SerializedName;

/**
 * Status object, that holds the result of the request.
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 *
 */
public final class StatusCode {

  @SerializedName("code")
  private int code;

  @SerializedName("reasonPhrase")
  private String reason;

  @SerializedName("details")
  private String details;

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(" Code: ").append(code).append("\n");
    sb.append(" Reason: ").append(reason).append("\n");
    sb.append(" Details: ").append(details).append("\n");

    return sb.toString();
  }
}
