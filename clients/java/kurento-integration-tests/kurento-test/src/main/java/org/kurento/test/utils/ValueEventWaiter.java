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

package org.kurento.test.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueEventWaiter<E> {

  private static final long DEFAULT_WAIT_MILLIS = 30000;

  private static final Logger log = LoggerFactory.getLogger(ValueEventWaiter.class);

  private CountDownLatch latch = new CountDownLatch(1);
  private String name;

  private E value;

  public ValueEventWaiter() {
    this("event");
  }

  public ValueEventWaiter(String name) {
    this.name = name;
  }

  public void eventReceived(E value) {
    this.value = value;
    latch.countDown();
  }

  public String getName() {
    return name;
  }

  public E waitFor() {
    return waitFor(DEFAULT_WAIT_MILLIS);
  }

  public E waitFor(E expectedValue) {
    return waitFor(DEFAULT_WAIT_MILLIS, expectedValue, true);
  }

  public E waitFor(long waitMillis, E expectedValue) {
    return waitFor(waitMillis, expectedValue, true);
  }

  public E waitFor(long waitMillis) {
    return waitFor(waitMillis, null, false);
  }

  private E waitFor(long waitMillis, E expectedValue, boolean honorExpectedValue) {

    log.debug("Start waiting {} ms for event '{}'", waitMillis, name);
    long start = System.currentTimeMillis();

    try {
      assertThat(latch.await(waitMillis, TimeUnit.MILLISECONDS))
          .as("Event '" + name + "' is received before " + waitMillis + " ms").isTrue();

      if (honorExpectedValue) {
        assertThat(value).isEqualTo(expectedValue);
      }

    } catch (InterruptedException e) {
      throw new RuntimeException("Waiting for event '" + name + "' has been interrupted");
    }

    long time = System.currentTimeMillis() - start;
    log.debug("Event '{}' is received in {} ms", name, time);

    return value;
  }

  public boolean hasBeenReceived() {
    return latch.getCount() == 0;
  }

}
