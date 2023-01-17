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

package org.kurento.test.internal;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kurento.test.grid.GridHandler;
import org.kurento.test.utils.SshConnection;

/**
 * Internal utility for killing the active processes of a user in the Selenium Grid hub (for manual
 * testing/debug purposes).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class KillActiveProcesses {

  private static final String REGEX = "id : http://(.*?), OS";

  public static void main(String[] args) throws IOException {

    for (String url : args) {
      String contents = GridHandler.readContents(url);

      Pattern p = Pattern.compile(REGEX);
      Matcher m = p.matcher(contents);

      String node;
      while (m.find()) {
        node = m.group();
        node = node.substring(12, node.lastIndexOf(":"));

        final String nodeFinal = node;
        Runnable run = new Runnable() {
          @Override
          public void run() {
            try {
              System.out.println("Killing " + nodeFinal);
              kill(nodeFinal);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        };
        new Thread(run).start();
      }
    }
  }

  public static void kill(String node) throws IOException {
    SshConnection remoteHost = new SshConnection(node);
    remoteHost.start();
    remoteHost.execCommand("kill", "-9", "-1");
  }

}
