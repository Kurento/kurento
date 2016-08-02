/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

package org.kurento.test.stability.pipeline;

import java.util.concurrent.CountDownLatch;

import org.kurento.client.ServerManager;
import org.kurento.test.base.StabilityTest;

/**
 * Base pipeline tests.
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.5.1
 */
public class BasePipeline extends StabilityTest {

  public int TIMEOUT = 30; // seconds

  private ServerManager serverManager;

  private double CONSTANT_MB = 1024;
  private long initMemory = 0;

  public BasePipeline() {
    setDeleteLogsIfSuccess(false);
  }

  public ServerManager getServerManager() {
    serverManager = kurentoClient.getServerManager();
    return serverManager;
  }

  public void initMemory() {
    getServerManager();
    initMemory = (long) (serverManager.getUsedMemory() / CONSTANT_MB);
  }

  public double getMemoryIncrease() {
    long endMemory = (long) (serverManager.getUsedMemory() / CONSTANT_MB);
    double increaseMemory = endMemory - initMemory;
    double percentageMemory = (increaseMemory * 100.0) / initMemory;

    log.debug("Init memory: {} Mb. End memory in this iteration: {} Mb", initMemory, endMemory);
    log.debug("Increase Memory: {} Mb. Increase: {} %", increaseMemory, percentageMemory);

    return percentageMemory;
  }

  /**
   * Auxiliary class that allows waiting that all objects are created or destroyed in the stress
   * tests. Also allow creating different latch with different values
   *
   * @author rbenitez
   *
   */
  class ObjectsLatch {
    private CountDownLatch objectsCreatedLatch;
    private CountDownLatch objectsDestroyedLatch;

    public ObjectsLatch(int count) {
      objectsCreatedLatch = new CountDownLatch(count);
      objectsDestroyedLatch = new CountDownLatch(count);
    }

    public CountDownLatch getObjectsCreatedLatch() {
      return objectsCreatedLatch;
    }

    public CountDownLatch getObjectsDestroyedLatch() {
      return objectsDestroyedLatch;
    }
  }

  /**
   * Auxiliary class that allows waiting that all objects are connected or disconnected in the
   * stress tests. Also allow creating different latch with different values
   *
   * @author rbenitez
   *
   */
  class ConnectionStateLatch {
    private CountDownLatch stateConnectedLatch;
    private CountDownLatch stateDisconnectedLatch;

    public ConnectionStateLatch(int count) {
      stateConnectedLatch = new CountDownLatch(count);
      stateDisconnectedLatch = new CountDownLatch(count);
    }

    public CountDownLatch getStateConnectedLatch() {
      return stateConnectedLatch;
    }

    public CountDownLatch getStateDisconnectedLatch() {
      return stateDisconnectedLatch;
    }
  }

}
