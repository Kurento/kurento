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

package org.kurento.test.grid;

import java.io.File;

import org.kurento.commons.exception.KurentoException;
import org.kurento.test.browser.BrowserType;
import org.kurento.test.utils.SshConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nodes in Selenium Grid testing.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class GridNode {

  private Logger log = LoggerFactory.getLogger(GridNode.class);

  private String host;
  private BrowserType browserType;
  private int maxInstances = 1;
  private boolean overwrite = false;
  private boolean started = false;
  private SshConnection ssh;
  private String home;
  private String tmpFolder;

  public GridNode(String host, BrowserType browserType, int maxInstances) {
    this.host = host;
    this.browserType = browserType;
    this.maxInstances = maxInstances;
    this.ssh = new SshConnection(host);
  }

  public GridNode(String host, BrowserType browserType, int maxInstances, String login,
      String passwd, String pem) {
    this.host = host;
    this.browserType = browserType;
    this.maxInstances = maxInstances;
    this.ssh = new SshConnection(host, login, passwd, pem);
  }

  public String getRemoteVideo(String video) {
    String remoteVideo = null;
    File file = new File(video);
    remoteVideo = getHome() + "/" + GridHandler.REMOTE_FOLDER + "/" + file.getName();
    return remoteVideo;
  }

  public void startSsh() {
    ssh.start();
    setTmpFolder(ssh.createTmpFolder());
  }

  public void stopSsh() {
    ssh.stop();
  }

  public SshConnection getSshConnection() {
    return ssh;
  }

  public String getTmpFolder() {
    return tmpFolder;
  }

  public void setTmpFolder(String tmpFolder) {
    this.tmpFolder = tmpFolder;
  }

  public String getHome() {
    if (home == null) {
      // OverThere SCP need absolute path, so home path must be known
      try {
        home = getSshConnection().execAndWaitCommandNoBr("echo", "~");
      } catch (KurentoException e) {
        log.error("Exception reading remote home " + e.getClass()
            + " ... returning default home value: ~");
        home = "~";
      }
    }
    return home;
  }

  public String getHost() {
    return host;
  }

  public BrowserType getBrowserType() {
    return browserType;
  }

  public int getMaxInstances() {
    return maxInstances;
  }

  public boolean isOverwrite() {
    return overwrite;
  }

  public boolean isStarted() {
    return started;
  }

  public void setStarted(boolean started) {
    this.started = started;
  }

}
