
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
