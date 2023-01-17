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

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.config.TestConfiguration.SELENIUM_HUB_PORT_DEFAULT;
import static org.kurento.test.config.TestConfiguration.SELENIUM_HUB_PORT_PROPERTY;

import org.kurento.test.grid.GridHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal utility for starting a Selenium Grid Hub (for manual testing/debug purposes).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class StartHub {

  public Logger log = LoggerFactory.getLogger(StartHub.class);

  public static void main(String[] args) throws Exception {
    int hubPort = getProperty(SELENIUM_HUB_PORT_PROPERTY, SELENIUM_HUB_PORT_DEFAULT);

    GridHub seleniumGridHub = new GridHub(hubPort);
    seleniumGridHub.start();
  }
}
