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

import org.openqa.grid.internal.utils.configuration.GridHubConfiguration;
import org.openqa.grid.web.Hub;
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
  private Hub hub;

  public GridHub(int port) {
    this.port = port;
    this.timeout = DEFAULT_TIMEOUT; // Default timeout
  }

  public void start() throws Exception {
    GridHubConfiguration config = new GridHubConfiguration();
    config.host = bindIp;
    config.port = this.port;
    config.timeout = getTimeout();

    hub = new Hub(config);
    log.debug("Starting hub on {}:{}", this.bindIp, this.port);
    hub.start();
  }

  public void stop() throws Exception {
    if (hub != null) {
      hub.stop();
    }
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

}
