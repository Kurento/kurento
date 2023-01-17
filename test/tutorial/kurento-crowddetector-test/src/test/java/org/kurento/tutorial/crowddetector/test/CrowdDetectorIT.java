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

package org.kurento.tutorial.crowddetector.test;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.demo.CrowdDetectorApp;
import org.kurento.test.base.BrowserTest;
import org.kurento.test.browser.WebPage;
import org.kurento.test.browser.WebPageType;
import org.kurento.test.config.TestScenario;
import org.kurento.test.services.KmsService;
import org.kurento.test.services.Service;
import org.kurento.test.services.WebServerService;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello World integration test.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 5.0.0
 */
public class CrowdDetectorIT extends BrowserTest<WebPage> {

  public static @Service(1) KmsService kms = new KmsService();
  public static @Service(2) WebServerService webServer =
      new WebServerService(CrowdDetectorApp.class);

  protected WebDriver driver;
  protected static final Logger log = LoggerFactory.getLogger(CrowdDetectorIT.class);

  protected final static int TEST_TIMEOUT = 30; // seconds
  protected final static int ALERT_TIMEOUT = 10; // seconds
  protected final static int PLAY_TIME = 5; // seconds

  @Before
  public void setup() {
    driver = this.getPage().getBrowser().getWebDriver();
  }

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChrome(WebPageType.ROOT);
  }

  @Test
  public void testCrowdDetector() throws InterruptedException {
    // Start application
    driver.findElement(By.id("start")).click();

    // Check alert
    WebDriverWait wait = new WebDriverWait(driver, ALERT_TIMEOUT);
    try {
      if (wait.until(ExpectedConditions.alertIsPresent()) != null) {
        driver.switchTo().alert().accept();
        driver.findElement(By.id("address")).sendKeys("rtsp://184.72.239.149/vod/mp4:BigBuckBunny_175k.mov");
        driver.findElement(By.id("changeFeed")).click();
        if (wait.until(ExpectedConditions.alertIsPresent()) != null) {
          driver.switchTo().alert().accept();
          driver.findElement(By.id("start")).click();
        }
      }
    } catch (TimeoutException te) {
      log.warn(te.getMessage());
    }

    // Assessment #1: Remote video tag should play media
    waitForStream("videoOutput");

    // Guard time to see application in action
    Thread.sleep(PLAY_TIME * 1000);

    // Stop application
    driver.findElement(By.id("stop")).click();
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
