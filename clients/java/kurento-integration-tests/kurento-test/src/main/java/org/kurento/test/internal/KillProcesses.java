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

import org.kurento.test.utils.SshConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal utility for killing all the processes of a user in a remote node (for manual
 * testing/debug purposes).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class KillProcesses {

  public static Logger log = LoggerFactory.getLogger(KillProcesses.class);

  public static void main(String[] args) throws IOException {
    for (String node : args) {
      if (SshConnection.ping(node)) {
        SshConnection remoteHost = new SshConnection(node);
        remoteHost.start();
        remoteHost.execCommand("kill", "-9", "-1");
      } else {
        log.error("Node down {}", node);
      }
    }
  }
}
