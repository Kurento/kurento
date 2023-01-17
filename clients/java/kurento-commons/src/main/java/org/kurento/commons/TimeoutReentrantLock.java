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

package org.kurento.commons;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TimeoutReentrantLock extends ReentrantLock {

  private static final long serialVersionUID = 3260128010629476025L;

  private long timeout;
  private String name;

  public TimeoutReentrantLock(long timeout, String name) {
    this.timeout = timeout;
    this.name = name;
  }

  public void tryLockTimeout(String method) {

    try {
      if (!tryLock(timeout, TimeUnit.MILLISECONDS)) {
        Thread ownerThread = getOwner();
        throw new TimeoutRuntimeException("Timeout waiting " + timeout + " millis "
            + "to acquire lock " + name + ". The lock is held by thread " + ownerThread.getName());
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("InterruptedException while trying to acquire lock", e);
    }
  }
}
