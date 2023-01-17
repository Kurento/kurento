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

package org.kurento.client.test;

import org.kurento.test.base.KurentoClientTest;

/**
 * Base for API tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class ApiBase extends KurentoClientTest {

  public static final String URL_BARCODES = "http://" + getTestFilesHttpPath()
      + "/video/filter/barcodes.webm";
  public static final String URL_FIWARECUT = "http://" + getTestFilesHttpPath()
      + "/video/filter/fiwarecut.webm";
  public static final String URL_SMALL = "http://" + getTestFilesHttpPath()
      + "/video/format/small.webm";
  public static final String URL_PLATES = "http://" + getTestFilesHttpPath()
      + "/video/filter/plates.webm";
  public static final String URL_POINTER_DETECTOR = "http://" + getTestFilesHttpPath()
      + "/video/filter/pointerDetector.mp4";

}
