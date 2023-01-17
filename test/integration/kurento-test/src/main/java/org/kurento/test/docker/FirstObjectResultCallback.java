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

package org.kurento.test.docker;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.core.async.ResultCallbackTemplate;

class FirstObjectResultCallback<E> extends ResultCallbackTemplate<FirstObjectResultCallback<E>, E> {

  private static final Logger log = LoggerFactory.getLogger(FirstObjectResultCallback.class);

  private E object;
  private CountDownLatch latch = new CountDownLatch(1);

  @Override
  public void onNext(E object) {
    this.object = object;
    latch.countDown();
    try {
      close();
    } catch (IOException e) {
      log.warn("Exception when closing stats cmd stream", e);
    }
  }

  public E waitForObject() throws InterruptedException {
    latch.await();
    return object;
  }
}