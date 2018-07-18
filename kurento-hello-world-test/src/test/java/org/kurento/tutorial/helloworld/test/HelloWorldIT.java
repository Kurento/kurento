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
 *
 */

package org.kurento.tutorial.helloworld.test;

import java.util.Collection;

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
import org.kurento.tutorial.helloworld.Application;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Hello World integration test.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
public class HelloWorldIT extends BrowserTest<WebPage> {

  public static @Service(1) KmsService kms = new KmsService();
  public static @Service(2) WebServerService webServer = new WebServerService(Application.class);

  private WebDriver driver;

  private final static int TEST_TIMEOUT = 10; // seconds
  private final static int PLAY_TIME = 5; // seconds

  @Before
  public void setup() {
    driver = this.getPage().getBrowser().getWebDriver();
  }

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChrome(WebPageType.ROOT);
  }

  @Test
  public void testHelloWorld() throws InterruptedException {
    // Start application
    driver.findElement(By.id("uiStartBtn")).click();

    // Assessment #1: Local video tag should play media
    waitForStream("uiLocalVideo");

    // Assessment #2: Remote video tag should play media
    waitForStream("uiRemoteVideo");

    // Guard time to see application in action
    Thread.sleep(PLAY_TIME * 1000);

    // Stop application
    driver.findElement(By.id("uiStopBtn")).click();
  }

  private void waitForStream(String videoTagId) throws InterruptedException {
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
