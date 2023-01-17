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

package org.kurento.test.base;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.kurento.commons.testing.SystemPerformanceTests;
import org.kurento.test.browser.WebRtcTestPage;
import org.kurento.test.monitor.SystemMonitorManager;

/**
 * Base for performance tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gsyc.es)
 * @since 4.2.5
 */
@Category(SystemPerformanceTests.class)
public class PerformanceTest extends KurentoClientBrowserTest<WebRtcTestPage> {

  protected SystemMonitorManager monitor;

  protected String monitorResultPath;

  protected boolean showLatency = false;

  public PerformanceTest() {
    setDeleteLogsIfSuccess(false);
  }

  @Before
  public void setupMonitor() {
    monitorResultPath = getDefaultOutputFile("-monitor.csv");
    monitor = new SystemMonitorManager();
    monitor.setShowLantency(showLatency);
    monitor.startMonitoring();
  }

  @After
  public void teardownMonitor() throws IOException {
    if (monitor != null) {
      monitor.stop();
      monitor.writeResults(monitorResultPath);
      monitor.destroy();
    }
  }

  public String getMonitorResultPath() {
    return monitorResultPath;
  }

  public void setMonitorResultPath(String monitorResultPath) {
    this.monitorResultPath = monitorResultPath;
  }

  public void setShowLatency(boolean showLatency) {
    this.showLatency = showLatency;
  }

}
