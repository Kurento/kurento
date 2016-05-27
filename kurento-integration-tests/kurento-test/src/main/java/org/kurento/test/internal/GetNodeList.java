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

package org.kurento.test.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kurento.test.grid.GridHandler;

/**
 * Internal utility for reading a node from a URL.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.0.1
 */
public class GetNodeList {

  public static void main(String[] args) throws IOException {
    List<String> nodeList = new ArrayList<>();

    for (String url : args) {
      String contents = GridHandler.readContents(url);
      Pattern p = Pattern.compile(GridHandler.IPS_REGEX);
      Matcher m = p.matcher(contents);
      while (m.find()) {
        nodeList.add(m.group());
      }
    }
    System.err.println(nodeList);
  }

}
