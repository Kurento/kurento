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

package org.kurento.test.browser;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.safari.SafariDriver;

/**
 * Browser to perform automated web testing with Selenium WebDriver.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public enum BrowserType {
  CHROME, FIREFOX, IEXPLORER, SAFARI;

  public Class<? extends WebDriver> getDriverClass() {
    switch (this) {
      case IEXPLORER:
        return InternetExplorerDriver.class;
      case FIREFOX:
        return FirefoxDriver.class;
      case SAFARI:
        return SafariDriver.class;
      case CHROME:
      default:
        return ChromeDriver.class;
    }
  }

  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }

}
