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

package org.kurento.client.internal;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.kurento.client.TFuture;
import org.kurento.client.TransactionExecutionException;
import org.kurento.client.TransactionRollbackException;
import org.kurento.client.internal.client.operation.Operation;
import org.kurento.client.internal.server.KurentoServerException;
import org.kurento.commons.exception.KurentoException;

import com.google.common.util.concurrent.SettableFuture;

public class TFutureImpl<V> implements TFuture<V> {

  private SettableFuture<V> future;
  private Operation operation;

  public TFutureImpl(Operation operation) {
    this.future = SettableFuture.create();
    this.operation = operation;
  }

  @Override
  public boolean isRollback() {
    return future.isCancelled();
  }

  @Override
  public boolean isCommitted() {
    return future.isDone();
  }

  @Override
  public V get() {
    try {
      return future.get();
    } catch (InterruptedException e) {
      throw new KurentoException(e);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof KurentoServerException) {
        throw new TransactionExecutionException(operation,
            ((KurentoServerException) e.getCause()).getError());
      } else {
        throw new KurentoException(e.getCause());
      }
    } catch (CancellationException e) {
      throw new TransactionRollbackException();
    }
  }

  public SettableFuture<V> getFuture() {
    return future;
  }

}
