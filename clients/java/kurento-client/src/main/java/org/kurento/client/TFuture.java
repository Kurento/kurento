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

package org.kurento.client;

public interface TFuture<V> {

  /**
   * Returns <tt>true</tt> if the transaction associated to this future was rolled back.
   *
   * @return <tt>true</tt> if the transaction associated to this future was rolled back.
   */
  boolean isRollback();

  /**
   * Returns <tt>true</tt> if the transaction associated to this future is committed. The
   * transaction can success or fail with exception, in all of these cases, this method will return
   * <tt>true</tt>.
   *
   * @return <tt>true</tt> if the transaction associated to this future is committed.
   */
  boolean isCommitted();

  /**
   * Waits if necessary for the transaction to be committed, and then retrieves its result.
   *
   * @return the transaction result
   * @throws java.util.concurrent.CancellationException
   *           if the transaction was cancelled with rollback
   * @throws java.util.concurrent.ExecutionException
   *           if the transaction threw an exception when committed
   * @throws InterruptedException
   *           if the current thread was interrupted while waiting
   */
  V get();

}
