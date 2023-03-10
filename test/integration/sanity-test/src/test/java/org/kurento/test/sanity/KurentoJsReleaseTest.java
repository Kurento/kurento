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

package org.kurento.test.sanity;

import static org.kurento.commons.PropertiesManager.getProperty;

import org.junit.Test;

/**
 * Sanity test for kurento-js releases.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class KurentoJsReleaseTest extends KurentoJsBase {

  public KurentoJsReleaseTest() {

    kurentoUrl = getProperty("bower.release.url", DEFAULT_BOWER_RELEASE_URL);
    log.debug("kurentoUrl = {}", kurentoUrl);
    if (!kurentoUrl.endsWith("/")) {
      kurentoUrl += "/";
    }
  }

  @Test
  public void kurentoJsReleaseTest() {
    doTest();
  }

}
