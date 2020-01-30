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

package org.kurento.test.config;

/**
 * Scope for browser: i) local (installed on machine running the tests; ii) remote (hosts acceded by
 * Selenium Grid); iii) In Saucelabs (a private PAAS for testing).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.1.0
 */
public enum BrowserScope {

  LOCAL, REMOTE, SAUCELABS, DOCKER, ELASTEST;

  @Override
  public String toString() {
    return this.name().toLowerCase();
  }

}
