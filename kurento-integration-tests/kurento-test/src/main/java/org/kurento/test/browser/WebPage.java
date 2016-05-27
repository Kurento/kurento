/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

package org.kurento.test.browser;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.kurento.test.latency.LatencyException;
import org.kurento.test.latency.VideoTag;
import org.kurento.test.monitor.PeerConnectionStats;
import org.kurento.test.monitor.SystemMonitorManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic web page for tests using Kurento test infrastructure.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.1.0
 */
public class WebPage {

  public static Logger log = LoggerFactory.getLogger(WebPage.class);

  public Browser browser;

  public Browser getBrowser() {
    return browser;
  }

  public void setBrowser(Browser browser) {
    this.browser = browser;
  }

  public void takeScreeshot(String file) throws IOException {
    File scrFile = ((TakesScreenshot) getBrowser().getWebDriver()).getScreenshotAs(OutputType.FILE);
    FileUtils.copyFile(scrFile, new File(file));
  }

  /*
   * setThresholdTime
   */
  public void setThresholdTime(int thresholdTime) {
    browser.setThresholdTime(thresholdTime);
  }

  /*
   * setColorCoordinates
   */
  public void setColorCoordinates(int x, int y) {
    browser.executeScript("kurentoTest.setColorCoordinates(" + x + "," + y + ");");
  }

  /*
   * checkColor
   */
  public void checkColor(String... videoTags) {
    String tags = "";
    for (String s : videoTags) {
      if (!tags.isEmpty()) {
        tags += ",";
      }
      tags += "'" + s + "'";
    }
    browser.executeScript("kurentoTest.checkColor(" + tags + ");");
  }

  /*
   * similarColorAt
   */
  public boolean similarColorAt(String videoTag, Color expectedColor, int x, int y) {
    setColorCoordinates(x, y);
    return similarColor(videoTag, expectedColor);

  }

  /*
   * similarColor
   */
  public boolean similarColor(String videoTag, Color expectedColor) {
    boolean out;
    final long endTimeMillis = System.currentTimeMillis() + browser.getTimeout() * 1000;

    boolean logWarn = true;
    while (true) {
      out = compareColor(videoTag, expectedColor, logWarn);
      if (out || System.currentTimeMillis() > endTimeMillis) {
        break;
      } else {
        // Polling: wait 200 ms and check again the color
        // Max wait = timeout variable
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
          log.trace("InterruptedException in guard condition ({})", e.getMessage());
        }
      }
      logWarn = false;
    }
    return out;
  }

  /*
   * compareColor
   */
  public boolean compareColor(String videoTag, Color expectedColor, boolean logWarn) {
    @SuppressWarnings("unchecked")
    List<Long> realColor = (List<Long>) browser.executeScriptAndWaitOutput(
        "return kurentoTest.colorInfo['" + videoTag + "'].currentColor;");

    long red = realColor.get(0);
    long green = realColor.get(1);
    long blue = realColor.get(2);

    double distance = Math.sqrt((red - expectedColor.getRed()) * (red - expectedColor.getRed())
        + (green - expectedColor.getGreen()) * (green - expectedColor.getGreen())
        + (blue - expectedColor.getBlue()) * (blue - expectedColor.getBlue()));

    String expectedColorStr = "[R=" + expectedColor.getRed() + ", G=" + expectedColor.getGreen()
        + ", B=" + expectedColor.getBlue() + "]";
    String realColorStr = "[R=" + red + ", G=" + green + ", B=" + blue + "]";
    boolean out = distance <= browser.getColorDistance();
    if (!out) {
      if (logWarn) {
        log.warn("Color NOT detected in video stream. Expected: {}, Real: {}", expectedColorStr,
            realColorStr);
      }
    } else {
      log.debug("Detected color in video stream. Expected: {}, Real: {}", expectedColorStr,
          realColorStr);
    }

    return out;
  }

  /*
   * activatePeerConnectionInboundStats
   */
  public void activatePeerConnectionInboundStats(String peerConnectionId) {
    activatePeerConnectionStats("activateInboundRtcStats", peerConnectionId);
  }

  /*
   * activatePeerConnectionOutboundStats
   */
  public void activatePeerConnectionOutboundStats(String peerConnectionId) {
    activatePeerConnectionStats("activateOutboundRtcStats", peerConnectionId);
  }

  private void activatePeerConnectionStats(String jsFunction, String peerConnectionId) {

    try {
      browser.executeScript("kurentoTest." + jsFunction + "('" + peerConnectionId + "');");

    } catch (WebDriverException we) {
      we.printStackTrace();

      // If client is not ready to gather rtc statistics, we just log it
      // as warning (it is not an error itself)
      log.warn("Client does not support RTC statistics (function kurentoTest.{}() not defined)",
          jsFunction);
    }
  }

  /**
   *
   * @param peerConnectionId
   */
  public void stopPeerConnectionInboundStats(String peerConnectionId) {
    stopPeerConnectionStats("stopInboundRtcStats", peerConnectionId);
  }

  /**
   *
   * @param peerConnectionId
   */
  public void stopPeerConnectionOutboundStats(String peerConnectionId) {
    stopPeerConnectionStats("stopOutboundRtcStats", peerConnectionId);
  }

  private void stopPeerConnectionStats(String jsFunction, String peerConnectionId) {

    try {
      log.info("kurentoTest." + jsFunction + "('" + peerConnectionId + "');");
      browser.executeScript("kurentoTest." + jsFunction + "('" + peerConnectionId + "');");

    } catch (WebDriverException we) {
      we.printStackTrace();

      // If client is not ready to gather rtc statistics, we just log it
      // as warning (it is not an error itself)
      log.warn("Client does not support RTC statistics (function kurentoTest.{}() not defined)");
    }
  }

  /*
   * getLatency
   */
  @SuppressWarnings("deprecation")
  public long getLatency() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final long[] out = new long[1];
    Thread t = new Thread() {
      @Override
      public void run() {
        Object latency = browser.executeScript("return kurentoTest.getLatency();");
        if (latency != null) {
          out[0] = (Long) latency;
        } else {
          out[0] = Long.MIN_VALUE;
        }
        latch.countDown();
      }
    };
    t.start();
    if (!latch.await(browser.getTimeout(), TimeUnit.SECONDS)) {
      t.interrupt();
      t.stop();
      throw new LatencyException("Timeout getting latency (" + browser.getTimeout() + "  seconds)");
    }
    return out[0];
  }

  public void waitColor(long timeoutSeconds, final VideoTag videoTag, final Color color) {
    WebDriverWait wait = new WebDriverWait(browser.getWebDriver(), timeoutSeconds);
    wait.until(new ExpectedCondition<Boolean>() {
      @Override
      public Boolean apply(WebDriver d) {
        return !((JavascriptExecutor) d).executeScript(videoTag.getColor()).equals(color);
      }
    });
  }

  /*
   * getCurrentTime
   */
  public long getCurrentTime(VideoTag videoTag) {
    Object time = browser.executeScript(videoTag.getTime());
    return time == null ? 0 : (Long) time;
  }

  /*
   * getCurrentColor
   */
  @SuppressWarnings("unchecked")
  public Color getCurrentColor(VideoTag videoTag) {
    return getColor((List<Long>) browser.executeScript(videoTag.getColor()));
  }

  private Color getColor(List<Long> color) {
    return new Color(color.get(0).intValue(), color.get(1).intValue(), color.get(2).intValue());
  }

  /*
   * checkLatencyUntil
   */
  public void checkLatencyUntil(SystemMonitorManager monitor, long endTimeMillis)
      throws InterruptedException, IOException {
    while (true) {
      if (System.currentTimeMillis() > endTimeMillis) {
        break;
      }
      Thread.sleep(100);
      try {
        long latency = getLatency();
        if (latency != Long.MIN_VALUE) {
          monitor.addCurrentLatency(latency);
        }
      } catch (LatencyException le) {
        monitor.incrementLatencyErrors();
      }
    }
  }

  /*
   * getRtcStats
   */
  @SuppressWarnings("unchecked")
  public PeerConnectionStats getRtcStats() {
    Map<String, Object> out = new HashMap<>();
    try {
      if (browser != null && browser.getWebDriver() != null) {
        out = (Map<String, Object>) browser.executeScript("return kurentoTest.rtcStats;");

        log.debug(">>>>>>>>>> kurentoTest.rtcStats {} {}", browser.getId(), out);
      }
    } catch (WebDriverException we) {
      // If client is not ready to gather rtc statistics, we just log it
      // as warning (it is not an error itself)
      log.warn("Client does not support RTC statistics" + " (variable rtcStats is not defined)");
    }
    return new PeerConnectionStats(out);
  }

  /*
   * activateLatencyControl
   */
  public void activateLatencyControl(String localId, String remoteId) {
    browser.executeScript(
        "kurentoTest.activateLatencyControl('" + localId + "', '" + remoteId + "');");

  }

  /*
   * getTimeout
   */
  public int getTimeout() {
    return browser.getTimeout();
  }

  /*
   * setTimeout
   */
  public void setTimeout(int timeoutSeconds) {
    browser.changeTimeout(timeoutSeconds);
  }

  /*
   * getThresholdTime
   */
  public int getThresholdTime() {
    return browser.getThresholdTime();

  }

  /*
   * equalDataChannelMessage
   */
  public boolean compareDataChannelMessage(String message) {
    boolean out;
    final long endTimeMillis = System.currentTimeMillis() + browser.getTimeout() * 1000;
    while (true) {
      String messageReceived = (String) browser
          .executeScript("return kurentoTest.getDataChannelMessage()");
      out = (message.equals(messageReceived));
      if (out || System.currentTimeMillis() > endTimeMillis) {
        break;
      } else {
        // Polling: wait 200 ms and check again the color
        // Max wait = timeout variable
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
          log.trace("InterruptedException in guard condition ({})", e.getMessage());
        }
      }
    }
    return out;
  }

  /*
   * syncTimeForOcr
   */
  public void syncTimeForOcr(String videoTagId, String peerConnectionId) {
    browser.executeScript(
        "kurentoTest.syncTimeForOcr('" + videoTagId + "', '" + peerConnectionId + "');");

    log.debug("Sync time in {} {}", browser.getId(), videoTagId);
    WebDriverWait wait = new WebDriverWait(browser.getWebDriver(), browser.getTimeout());
    wait.until(new ExpectedCondition<Boolean>() {
      @Override
      public Boolean apply(WebDriver d) {
        return (Boolean) ((JavascriptExecutor) d).executeScript("return kurentoTest.sync;");
      }
    });
    log.debug("[Done] Sync time in {} {}", browser.getId(), videoTagId);
  }

  /*
   * startOcr
   */
  public void startOcr() {
    browser.executeScript("kurentoTest.startOcr();");
  }

  /*
   * endOcr
   */
  public void endOcr() {
    browser.executeScript("kurentoTest.endOcr();");
  }

  /*
   * getOcr
   */
  @SuppressWarnings("unchecked")
  public Map<String, String> getOcr() {
    return new TreeMap<String, String>(
        (Map<String, String>) browser.executeScript("return kurentoTest.ocrImageMap;"));
  }

  /*
   * getStatsList
   */
  @SuppressWarnings("unchecked")
  public List<Map<String, String>> getStatsList() {
    return (List<Map<String, String>>) browser.executeScript("return kurentoTest.rtcStatsList;");
  }

}
