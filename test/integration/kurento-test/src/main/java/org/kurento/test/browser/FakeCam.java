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

import org.kurento.test.utils.Shell;

/**
 * Fake cam singleton.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 * @see <a href="https://github.com/umlaeute/v4l2loopback">v4l2loopback</a>
 */
public class FakeCam {

  private static FakeCam singleton = null;

  /**
   * From 1 to NUM_FAKE_CAMS.
   */
  private static int NUM_FAKE_CAMS = 4;

  private int currentCam;

  public static FakeCam getSingleton() {
    if (singleton == null) {
      singleton = new FakeCam();
    }
    return singleton;
  }

  public FakeCam() {
    this.currentCam = 0;
  }

  public int getCam() {
    this.currentCam++;
    if (this.currentCam > NUM_FAKE_CAMS) {
      throw new IndexOutOfBoundsException();
    }
    return this.currentCam;
  }

  public void launchCam(String video) {
    Shell.runAndWait("sh", "-c", "gst-launch filesrc location=" + video
        + " ! decodebin2 ! v4l2sink device=/dev/video" + getCam());
  }

}
