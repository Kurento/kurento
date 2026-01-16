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

package org.kurento.test.grid;

import org.openqa.selenium.grid.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selenium Grid Hub.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class GridHub {

  public static Logger log = LoggerFactory.getLogger(GridHub.class);

  private static final int DEFAULT_TIMEOUT = 60;

  private String bindIp = "0.0.0.0";
  private int port;
  private int timeout;
  private Thread hubThread;

  public GridHub(int port) {
    this.port = port;
    this.timeout = DEFAULT_TIMEOUT; // Default timeout
  }

  public void start() throws Exception {
    log.debug("Starting hub on {}:{}", this.bindIp, this.port);
    hubThread = new Thread(() -> {
      try {
        Main.main(new String[] { "hub", "--port", String.valueOf(port) });
      } catch (Exception e) {
        log.error("Exception starting Selenium Grid Hub", e);
      }
    });
    hubThread.start();
  }

  public void stop() throws Exception {
    if (hubThread != null) {
      hubThread.interrupt();
    }
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

}
