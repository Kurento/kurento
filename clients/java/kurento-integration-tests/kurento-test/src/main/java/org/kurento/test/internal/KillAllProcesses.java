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

package org.kurento.test.internal;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.kurento.test.grid.GridHandler;
import org.kurento.test.utils.SshConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal utility for killing all the processes of a user in a remote node (for manual
 * testing/debug purposes).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class KillAllProcesses {

  public static Logger log = LoggerFactory.getLogger(KillAllProcesses.class);

  public static void main(String[] args) throws InterruptedException {
    List<String> nodeList = GridHandler.getInstance().getNodeList();

    int nodeListSize = nodeList.size();
    log.debug("Node availables in the node list: {}", nodeListSize);
    ExecutorService executor = Executors.newFixedThreadPool(nodeListSize);
    final CountDownLatch latch = new CountDownLatch(nodeListSize);

    for (final String node : nodeList) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          Thread.currentThread().setName(node);

          if (SshConnection.ping(node)) {
            SshConnection remoteHost = null;
            try {
              log.debug("Openning connection to node {}", node);
              remoteHost = new SshConnection(node);
              remoteHost.start();
              remoteHost.execCommand("kill", "-9", "-1");
            } catch (Throwable e) {
              e.printStackTrace();
            } finally {
              if (remoteHost != null) {
                log.debug("Closing connection to node {}", node);
                remoteHost.stop();
              }
            }
          } else {
            log.error("Node down {}", node);
          }

          latch.countDown();
        }
      });
    }

    latch.await();
    executor.shutdown();
  }
}
