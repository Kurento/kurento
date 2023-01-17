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

package org.kurento.test.sdp;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for handling SDPs.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.1.0
 */
public class SdpUtils {

  private static final String SDP_DELIMITER = "\r\n";

  public static final Logger log = LoggerFactory.getLogger(SdpUtils.class);

  public static String mangleSdp(String sdpIn, String[] removeCodes) {
    String sdpMangled1 = "";
    List<String> indexList = new ArrayList<>();
    for (String line : sdpIn.split(SDP_DELIMITER)) {
      boolean codecFound = false;
      for (String codec : removeCodes) {
        codecFound |= line.contains(codec);
      }
      if (codecFound) {
        String index = line.substring(line.indexOf(":") + 1, line.indexOf(" ") + 1);
        indexList.add(index);
      } else {
        sdpMangled1 += line + SDP_DELIMITER;
      }
    }

    String sdpMangled2 = "";
    log.debug("indexList " + indexList);
    for (String line : sdpMangled1.split(SDP_DELIMITER)) {
      for (String index : indexList) {
        line = line.replaceAll(index, "");
      }
      sdpMangled2 += line + SDP_DELIMITER;
    }
    return sdpMangled2;
  }

}
