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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DefaultContinuation<F> implements Continuation<F> {

  protected static final Logger log = LoggerFactory.getLogger(DefaultContinuation.class);
  private final Continuation<?> cont;

  public DefaultContinuation(Continuation<?> cont) {
    this.cont = cont;
  }

  @Override
  public abstract void onSuccess(F result) throws Exception;

  @Override
  public void onError(Throwable cause) {
    try {
      cont.onError(cause);
    } catch (Exception e) {
      log.warn("[Continuation] error invoking onError implemented by client", e);
    }
  }

}
