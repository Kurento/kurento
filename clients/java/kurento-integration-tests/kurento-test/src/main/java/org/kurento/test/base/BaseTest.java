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

package org.kurento.test.base;

import static org.kurento.test.config.Protocol.FILE;
import static org.kurento.test.config.Protocol.HTTP;
import static org.kurento.test.config.Protocol.S3;

import org.kurento.test.browser.WebRtcTestPage;
import org.kurento.test.config.Protocol;

/**
 * Base for player tests.
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.3.1
 */
public class BaseTest extends KurentoClientBrowserTest<WebRtcTestPage> {

  public BaseTest() {
  }

  public String getMediaUrl(Protocol protocol, String nameMedia) {
    String mediaUrl = "";
    switch (protocol) {
      case HTTP:
        mediaUrl = HTTP + "://" + getTestFilesHttpPath();
        break;
      case FILE:
        mediaUrl = FILE + "://" + getTestFilesDiskPath();
        break;
      case S3:
        mediaUrl = S3 + "://" + getTestFilesS3Path();
        break;
      default:
        throw new RuntimeException(protocol + "is not supported in this test.");
    }
    return mediaUrl + nameMedia;
  }
}
