/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */

package org.kurento.tutorial.one2manycall.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.base.BrowserTest;
import org.kurento.test.browser.WebPage;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.config.TestScenario;
import org.kurento.test.services.KmsService;
import org.kurento.test.services.Service;
import org.kurento.test.services.WebServerService;
import org.kurento.tutorial.one2manycall.One2ManyCallApp;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * One to many call integration test.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
public class One2ManyCallIT extends BrowserTest<WebPage> {

  public static @Service(1) KmsService kms = new KmsService();
  public static @Service(2) WebServerService webServer =
      new WebServerService(One2ManyCallApp.class);

  protected WebDriver master;
  protected List<WebDriver> viewers;

  protected final static int TEST_TIMEOUT = 30; // seconds
  protected final static int PLAY_TIME = 5; // seconds
  protected final static String DEFAULT_NUM_VIEWERS = "3";
  protected final static String APP_URL = "https://localhost:8443/";
  protected static int numViewers = 1;

  @Before
  public void setup() {
    master = this.getPage(0).getBrowser().getWebDriver();

    viewers = new ArrayList<>(numViewers);
    for (int i = 0; i < numViewers; i++) {
      viewers.add(this.getPage(i + 1).getBrowser().getWebDriver());
    }
  }

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    numViewers = Integer.parseInt(System.getProperty("test.num.viewers", DEFAULT_NUM_VIEWERS));
    return TestScenario.localChromes(numViewers + 1, WebPageType.ROOT);
  }

  @Test
  public void testOne2Many() throws InterruptedException {
    // Start application as master
    master.findElement(By.id("presenter")).click();

    // Assessment #1: Master video tag should play media
    waitForStream(master, "video");

    // VIEWERS
    for (WebDriver viewer : viewers) {

      // Start application as viewer
      viewer.findElement(By.id("viewer")).click();

      // Assessment #2: Viewer video tag should play media
      waitForStream(viewer, "video");
    }

    // Guard time to see application in action
    Thread.sleep(PLAY_TIME * 1000);

    // Stop application (master)
    master.findElement(By.id("stop")).click();
  }

  private void waitForStream(WebDriver driver, String videoTagId) throws InterruptedException {
    WebElement video = driver.findElement(By.id(videoTagId));
    int i = 0;
    for (; i < TEST_TIMEOUT; i++) {
      Number currentTime = (Number) ((JavascriptExecutor) driver)
          .executeScript("return arguments[0].currentTime;", video);
      if (currentTime.intValue() > 0) {
        break;
      } else {
        Thread.sleep(1000);
      }
    }
    if (i == TEST_TIMEOUT) {
      Assert.fail(
          "Video tag '" + videoTagId + "' is not playing media after " + TEST_TIMEOUT + " seconds");
    }
  }

}
