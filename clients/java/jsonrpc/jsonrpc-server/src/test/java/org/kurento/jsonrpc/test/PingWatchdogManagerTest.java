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

package org.kurento.jsonrpc.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.kurento.jsonrpc.internal.server.PingWatchdogManager;
import org.kurento.jsonrpc.internal.server.PingWatchdogManager.NativeSessionCloser;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class PingWatchdogManagerTest {

  @Test
  public void test() throws InterruptedException {

    ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
    executor.initialize();

    NativeSessionCloser closer = mock(NativeSessionCloser.class);
    PingWatchdogManager manager = new PingWatchdogManager(executor, closer);

    manager.setPingWatchdog(true);

    for (int i = 0; i < 10; i++) {
      manager.pingReceived("TransportID", 100);
      Thread.sleep(100);
    }

    Thread.sleep(500);

    verify(closer).closeSession("TransportID");
  }

}
