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

package org.kurento.client.internal.client;

import org.kurento.client.Continuation;
import org.kurento.jsonrpc.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteObjectFactory {

  private static final Logger log = LoggerFactory.getLogger(RemoteObjectFactory.class);

  private final RomClient client;
  private final RomManager manager;

  public RemoteObjectFactory(RomClient client) {
    this.client = client;
    this.manager = new RomManager(client);
  }

  public RemoteObject create(String remoteClassName, Props constructorParams, Props genericProps) {

    String objectRef = client.create(remoteClassName, constructorParams, genericProps);

    return new RemoteObject(objectRef, remoteClassName, manager);
  }

  public RemoteObject create(String remoteClassName) {
    return create(remoteClassName, (Props) null, (Props) null);
  }

  public void create(final String remoteClassName, final Props constructorParams,
      Props genericProps, final Continuation<RemoteObject> cont) {

    client.create(remoteClassName, constructorParams, genericProps, new Continuation<String>() {
      @Override
      public void onSuccess(String objectRef) {
        try {
          cont.onSuccess(new RemoteObject(objectRef, remoteClassName, manager));
        } catch (Exception e) {
          log.warn("[Continuation] error invoking onSuccess implemented by client", e);
        }
      }

      @Override
      public void onError(Throwable cause) {
        try {
          cont.onError(cause);
        } catch (Exception e) {
          log.warn("[Continuation] error invoking onError implemented by client", e);
        }
      }
    });
  }

  public void create(String remoteClassName, Continuation<RemoteObject> cont) {
    create(remoteClassName, null, null, cont);
  }

  public void destroy() {
    this.client.destroy();
  }

  public RomManager getManager() {
    return manager;
  }
}