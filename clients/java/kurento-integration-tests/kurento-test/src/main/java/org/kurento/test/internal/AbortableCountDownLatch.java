/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

package org.kurento.test.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Abortable count down latch. If the latch is aborted, all threads will be signaled to continue and
 * the await method will raise a {@link AbortedException}
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Ivan Gracia (igracia@kurento.org)
 * @since 6.0.0
 */
public class AbortableCountDownLatch extends CountDownLatch {
  private boolean aborted;

  private long remainingLatchesCount;

  private String message;
  private Throwable throwable;

  public AbortableCountDownLatch(int count) {
    super(count);
  }

  /**
   * Unblocks all threads waiting on this latch and cause them to receive an AbortedException. If
   * the latch has already counted all the way down, this method does nothing.
   */
  // public void abort() {
  // abort(null, null);
  // }

  public void abort(String message, Throwable throwable) {

    this.message = message;
    this.throwable = throwable;

    if (getCount() == 0) {
      return;
    }

    this.aborted = true;
    this.remainingLatchesCount = getCount();
    while (getCount() > 0) {
      countDown();
    }
  }

  @Override
  public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
    final boolean rtrn = super.await(timeout, unit);
    if (aborted) {
      throw new AbortedException(message, throwable);
    }
    return rtrn;
  }

  @Override
  public void await() throws InterruptedException {
    super.await();
    if (aborted) {
      throw new AbortedException(message, throwable);
    }
  }

  public long getRemainingLatchesCount() {
    return remainingLatchesCount;
  }

  public static class AbortedException extends InterruptedException {

    private static final long serialVersionUID = 5426681873843162292L;

    private Throwable cause;

    public AbortedException() {
    }

    public AbortedException(String detailMessage) {
      super(detailMessage);
    }

    public AbortedException(String detailMessage, Throwable cause) {
      super(detailMessage);
      this.cause = cause;
    }

    @Override
    public synchronized Throwable getCause() {
      return cause;
    }
  }

}
