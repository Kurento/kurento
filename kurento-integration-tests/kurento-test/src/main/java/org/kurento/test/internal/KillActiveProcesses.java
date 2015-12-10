/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
