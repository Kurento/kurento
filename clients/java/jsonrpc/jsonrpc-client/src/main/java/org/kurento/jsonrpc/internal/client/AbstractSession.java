/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

package org.kurento.jsonrpc.internal.client;

import org.kurento.jsonrpc.Session;

public abstract class AbstractSession implements Session {

  private String sessionId;
  private Object registerInfo;
  private boolean newSession = true;

  public AbstractSession(String sessionId, Object registerInfo) {
    this.sessionId = sessionId;
    this.registerInfo = registerInfo;
  }

  @Override
  public Object getRegisterInfo() {
    return registerInfo;
  }

  public void setRegisterInfo(Object registerInfo) {
    this.registerInfo = registerInfo;
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  @Override
  public boolean isNew() {
    return newSession;
  }

  public void setNew(boolean newSession) {
    this.newSession = newSession;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (sessionId == null ? 0 : sessionId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AbstractSession other = (AbstractSession) obj;
    if (sessionId == null) {
      if (other.sessionId != null) {
        return false;
      }
    } else if (!sessionId.equals(other.sessionId)) {
      return false;
    }
    return true;
  }

}
