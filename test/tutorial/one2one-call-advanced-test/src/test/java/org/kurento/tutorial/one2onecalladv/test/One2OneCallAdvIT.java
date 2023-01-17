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

package org.kurento.tutorial.one2onecalladv.test;

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
import org.kurento.tutorial.one2onecalladv.One2OneCallAdvApp;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Advanced one to one call integration test.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
public class One2OneCallAdvIT extends BrowserTest<WebPage> {

  public static @Service(1) KmsService kms = new KmsService();
  public static @Service(2) WebServerService webServer =
      new WebServerService(One2OneCallAdvApp.class);

  protected WebDriver caller;
  protected WebDriver callee;

  protected final static int TEST_TIMEOUT = 30; // seconds
  protected final static int PLAY_TIME = 10; // seconds
  protected final static String APP_URL = "https://localhost:8443/";
  protected final static String CALLER_NAME = "user1";
  protected final static String CALLEE_NAME = "user2";

  @Before
  public void setup() {
    caller = this.getPage(0).getBrowser().getWebDriver();
    callee = this.getPage(1).getBrowser().getWebDriver();
  }

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromes(2, WebPageType.ROOT);
  }

  @Test
  public void testAdvancedOne2One() throws InterruptedException {
    // Register caller
    caller.findElement(By.id("name")).sendKeys(CALLER_NAME);
    caller.findElement(By.id("register")).click();

    // Register caller
    callee.findElement(By.id("name")).sendKeys(CALLEE_NAME);
    callee.findElement(By.id("register")).click();

    // Caller calls callee
    caller.findElement(By.id("peer")).sendKeys(CALLEE_NAME);
    caller.findElement(By.id("call")).click();

    // Callee accepts call
    waitForIncomingCallDialog(callee);
    callee.switchTo().alert().accept();

    // Assertions #1: local and remote video tags of caller and callee
    // should play media
    waitForStream(caller, "videoInput");
    waitForStream(caller, "videoOutput");
    waitForStream(callee, "videoInput");
    waitForStream(callee, "videoOutput");

    // Guard time to see application (1st part, one2one) in action
    Thread.sleep(PLAY_TIME * 1000);

    // Stop application by caller
    caller.findElement(By.id("terminate")).click();

    // Watch recordings
    callee.findElement(By.id("peer")).sendKeys(CALLER_NAME);
    callee.findElement(By.id("play")).click();
    caller.findElement(By.id("play")).click();

    // Assertions #2: local and remote video tags of caller and callee
    // should play media
    waitForStream(caller, "videoOutput");
    waitForStream(callee, "videoOutput");

    // Guard time to see application (2nd part, play recordings) in action
    Thread.sleep(PLAY_TIME * 1000);
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

  private void waitForIncomingCallDialog(WebDriver driver) throws InterruptedException {
    int i = 0;
    for (; i < TEST_TIMEOUT; i++) {
      try {
        driver.switchTo().alert();
        break;
      } catch (NoAlertPresentException e) {
        Thread.sleep(1000);
      }
    }
    if (i == TEST_TIMEOUT) {
      throw new RuntimeException(
          "Timeout (" + TEST_TIMEOUT + " seconds) waiting for incoming call");
    }
  }

}
