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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.kurento.test.utils.SshConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

/**
 * Internal test application for assessing the state of hosts for nodes in Selenium Grid.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class CheckNodes {

  public Logger log = LoggerFactory.getLogger(CheckNodes.class);

  public void check() throws IOException {
    InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("node-list.txt");
    List<String> nodeList =
        CharStreams.readLines(new InputStreamReader(inputStream, Charsets.UTF_8));

    List<String> nodesWithoutXvfb = new ArrayList<String>();
    List<String> nodesWithException = new ArrayList<String>();
    List<String> nodesDown = new ArrayList<String>();
    List<String> nodesOk = new ArrayList<String>();

    for (String node : nodeList) {
      if (SshConnection.ping(node)) {

        SshConnection remoteHost = new SshConnection(node);
        try {
          remoteHost.start();
          int xvfb = remoteHost.runAndWaitCommand("xvfb-run");
          if (xvfb != 2) {
            nodesWithoutXvfb.add(node);
          } else {
            nodesOk.add(node);
          }
          log.debug("{} {}", node, xvfb);
        } catch (Exception e) {
          log.error("Exception in node {} : {}", node, e.getClass());
          nodesWithException.add(node);
        } finally {
          remoteHost.stop();
        }
      } else {
        log.error("Node down {}", node);
        nodesDown.add(node);
      }
    }

    log.debug("Nodes Ok: {} {}", nodesOk.size(), nodesOk);
    log.debug("Nodes without Xvfb: {} {}", nodesWithoutXvfb.size(), nodesWithoutXvfb);
    log.debug("Nodes with exception: {} {}", nodesWithException.size(), nodesWithException);
    log.debug("Nodes down: {} {}", nodesDown.size(), nodesDown);
  }

  public static void main(String[] args) throws IOException {
    CheckNodes checkNodes = new CheckNodes();
    checkNodes.check();
  }

}
