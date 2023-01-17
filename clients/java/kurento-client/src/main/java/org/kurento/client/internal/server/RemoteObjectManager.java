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

package org.kurento.client.internal.server;

import org.kurento.client.internal.transport.serialization.ObjectRefsManager;
import org.kurento.commons.SecretGenerator;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class RemoteObjectManager implements ObjectRefsManager {

  // This class is used to control equals behavior of values in the biMap
  // regardless equals in remote classes
  public static class ObjectHolder {
    private Object object;

    public ObjectHolder(Object object) {
      this.object = object;
    }

    public Object getObject() {
      return object;
    }

    @Override
    public int hashCode() {
      return object.hashCode();
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
      ObjectHolder other = (ObjectHolder) obj;
      return object == other.object;
    }
  }

  private SecretGenerator secretGenerator = new SecretGenerator();
  private BiMap<String, ObjectHolder> remoteObjects = HashBiMap.create();

  public String putObject(Object object) {
    String nextSecret;
    do {
      nextSecret = secretGenerator.nextSecret();
    } while (remoteObjects.get(nextSecret) != null);

    remoteObjects.put(nextSecret, new ObjectHolder(object));

    return nextSecret;
  }

  @Override
  public Object getObject(String objectRef) {
    return remoteObjects.get(objectRef).getObject();
  }

  public void releaseObject(String objectRef) {
    this.remoteObjects.remove(objectRef);
  }

  public String getObjectRefFrom(Object object) {
    return remoteObjects.inverse().get(new ObjectHolder(object));
  }

}
