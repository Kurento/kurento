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

package org.kurento.test.latency;

import java.awt.Color;
import java.text.SimpleDateFormat;

import org.kurento.test.browser.WebPage;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread to detect change of color in one of the video tags (local or remote) of the browser.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class ColorTrigger implements Runnable {

  public Logger log = LoggerFactory.getLogger(ColorTrigger.class);
  private VideoTag videoTag;
  private WebPage testClient;
  private Color color = Color.BLACK; // Initial color
  private ChangeColorObservable observable;
  private long timeoutSeconds;

  public ColorTrigger(VideoTag videoTag, WebPage testClient, ChangeColorObservable observable,
      long timeoutSeconds) {
    this.videoTag = videoTag;
    this.testClient = testClient;
    this.observable = observable;
    this.timeoutSeconds = timeoutSeconds;
  }

  @Override
  public void run() {
    while (true) {
      try {
        testClient.waitColor(timeoutSeconds, videoTag, color);
        Color currentColor = testClient.getCurrentColor(videoTag);

        if (!currentColor.equals(color)) {
          long changeTimeMilis = testClient.getCurrentTime(videoTag);
          String parsedtime = new SimpleDateFormat("mm:ss.SSS").format(changeTimeMilis);

          log.debug("Color changed on {} from {} to {} at minute {}", videoTag, color, currentColor,
              parsedtime);
          color = currentColor;

          ChangeColorEvent event = new ChangeColorEvent(videoTag, changeTimeMilis, color);
          observable.detectedColorChange(event);
        }
      } catch (WebDriverException we) {
        // This kind of exception can occur but does not matter for the
        // execution of the test
      } catch (Exception e) {
        break;
      }
    }
  }

}
