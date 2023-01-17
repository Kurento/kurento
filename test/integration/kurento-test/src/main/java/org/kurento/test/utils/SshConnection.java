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

package org.kurento.test.utils;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.config.TestConfiguration.TEST_NODE_LOGIN_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_NODE_PASSWD_PROPERTY;
import static org.kurento.test.config.TestConfiguration.TEST_NODE_PEM_PROPERTY;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.kurento.commons.exception.KurentoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;
import com.xebialabs.overthere.CmdLine;
import com.xebialabs.overthere.ConnectionOptions;
import com.xebialabs.overthere.OperatingSystemFamily;
import com.xebialabs.overthere.Overthere;
import com.xebialabs.overthere.OverthereConnection;
import com.xebialabs.overthere.OverthereFile;
import com.xebialabs.overthere.OverthereProcess;
import com.xebialabs.overthere.ssh.SshConnectionBuilder;
import com.xebialabs.overthere.ssh.SshConnectionType;

/**
 * SSH connection to a remote host.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
public class SshConnection {

  public static Logger log = LoggerFactory.getLogger(SshConnection.class);
  public static final String DEFAULT_TMP_FOLDER = "/tmp";

  private static final int NODE_INITIAL_PORT = 5555;
  private static final int PING_TIMEOUT = 2; // seconds
  private static final int DEFAULT_CONNECTION_TIMEOUT = 30000; // ms

  private String host;
  private String login;
  private String passwd;
  private String pem;
  private String tmpFolder;
  private int connectionTimeout;
  private OverthereConnection connection;

  public SshConnection(String host) {
    this.host = host;
    this.login = getProperty(TEST_NODE_LOGIN_PROPERTY);
    String pem = getProperty(TEST_NODE_PEM_PROPERTY);
    if (pem != null) {
      this.pem = pem;
    } else {
      this.passwd = getProperty(TEST_NODE_PASSWD_PROPERTY);
    }
    this.connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
  }

  public SshConnection(String host, String login, String passwd, String pem) {
    this.host = host;
    this.login = login;
    if (pem != null) {
      this.pem = pem;
    } else {
      this.passwd = passwd;
    }
  }

  public List<String> listFiles(String folder, boolean recursive, boolean includeFolders) {

    String[] command = null;
    if (recursive && includeFolders) {
      command = new String[] { "find", folder };
    } else if (recursive && !includeFolders) {
      command = new String[] { "find", folder, "-type", "f" };
    } else if (!recursive && includeFolders) {
      command = new String[] { "find", folder, "-maxdepth", "1" };
    } else if (!recursive && !includeFolders) {
      command = new String[] { "find", folder, "-maxdepth", "1", "-type", "f" };
    }
    return Arrays.asList(execAndWaitCommand(command).split("\r\n"));
  }

  public void mkdirs(String dir) {
    execAndWaitCommand("mkdir", "-p", dir);
  }

  public String createTmpFolder() {
    try {
      do {
        tmpFolder = DEFAULT_TMP_FOLDER + "/" + System.nanoTime();
      } while (exists(tmpFolder));
      execAndWaitCommand("mkdir", tmpFolder);
    } catch (IOException e) {
      tmpFolder = DEFAULT_TMP_FOLDER;
    }

    log.debug("Remote folder to store temporal files in node {}: {} ", host, tmpFolder);
    return tmpFolder;
  }

  public void getFile(String targetFile, String origFile) {
    log.debug("Getting remote file: {} (in host {}) to local file: {}", origFile, host, targetFile);

    OverthereFile motd = connection.getFile(origFile);
    if (!motd.isDirectory()) {
      InputStream is = motd.getInputStream();
      try {
        Files.copy(is, Paths.get(targetFile), StandardCopyOption.REPLACE_EXISTING);
        is.close();
      } catch (IOException e) {
        log.error("Exception getting file: {} to {} ({})", origFile, targetFile, e.getMessage());
      }
    }
  }

  public void scp(String origFile, String targetFile) {
    log.debug("Copying local file: {} to remote file: {} (in host {})", origFile, targetFile, host);

    OverthereFile motd = connection.getFile(targetFile);
    OutputStream w = motd.getOutputStream();

    try {
      byte[] origBytes = Files.readAllBytes(Paths.get(origFile));
      w.write(origBytes);
      w.close();
    } catch (IOException e) {
      throw new KurentoException("Exception in SCP " + origFile + " " + targetFile, e);
    }

  }

  public void start() {
    ConnectionOptions options = new ConnectionOptions();
    if (pem != null) {
      options.set(SshConnectionBuilder.PRIVATE_KEY_FILE, pem);
    } else {
      options.set(ConnectionOptions.PASSWORD, passwd);
    }

    options.set(ConnectionOptions.CONNECTION_TIMEOUT_MILLIS, connectionTimeout);
    options.set(ConnectionOptions.USERNAME, login);
    options.set(ConnectionOptions.ADDRESS, host);
    options.set(ConnectionOptions.OPERATING_SYSTEM, OperatingSystemFamily.UNIX);
    options.set(SshConnectionBuilder.CONNECTION_TYPE, SshConnectionType.SCP);

    connection = Overthere.getConnection(SshConnectionBuilder.SSH_PROTOCOL, options);

  }

  public boolean isStarted() {
    return connection != null;
  }

  public void stop() {
    if (isStarted()) {
      connection.close();
      connection = null;
    }
  }

  public void execCommand(final String... command) {
    if (connection.canStartProcess()) {
      connection.startProcess(CmdLine.build(command));
    }
  }

  public int runAndWaitCommand(String... command) {
    return connection.execute(CmdLine.build(command));
  }

  public String execAndWaitCommand(String... command) {
    log.debug("execAndWaitCommand: {} ", Arrays.toString(command));

    CmdLine cmdLine = new CmdLine();
    for (String c : command) {
      cmdLine.addRaw(c);
    }
    OverthereProcess process = connection.startProcess(cmdLine);

    StringBuilder sb = new StringBuilder();
    try {
      BufferedReader r = new BufferedReader(new InputStreamReader(process.getStdout(), "UTF-8"));
      String line = null;
      while ((line = r.readLine()) != null) {
        log.debug(line);
        sb.append(line).append("\r\n");
      }
    } catch (Exception e) {
      throw new KurentoException("Exception executing command " + Arrays.toString(command), e);
    }

    return sb.toString();
  }

  public String execAndWaitCommandWithStderr(String... command) throws IOException {
    OverthereProcess process = connection.startProcess(CmdLine.build(command));
    String result = CharStreams.toString(new InputStreamReader(process.getStdout(), "UTF-8"));
    result += CharStreams.toString(new InputStreamReader(process.getStderr(), "UTF-8"));
    return result;
  }

  public String execAndWaitCommandNoBr(String... command) {
    return execAndWaitCommand(command).replace("\n", "").replace("\r", "");
  }

  public boolean exists(String fileOrFolder) throws IOException {
    String output = execAndWaitCommand("file", fileOrFolder);
    return !output.contains("ERROR");
  }

  public int getFreePort() throws IOException {
    int port = NODE_INITIAL_PORT - 1;
    String output;
    do {
      port++;
      output = execAndWaitCommand("netstat", "-auxn");
    } while (output.contains(":" + port));
    return port;
  }

  public static boolean ping(String ipAddress) {
    return ping(ipAddress, SshConnection.PING_TIMEOUT);
  }

  public static boolean ping(final String ipAddress, int timeout) {
    final CountDownLatch latch = new CountDownLatch(1);

    Thread t = new Thread() {
      @Override
      public void run() {
        try {
          String[] command = { "ping", "-c", "1", ipAddress };
          Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
          CharStreams.toString(new InputStreamReader(p.getInputStream(), "UTF-8"));
          latch.countDown();
        } catch (Exception e) { // Intentionally left blank
        }
      }
    };
    t.setDaemon(true);
    t.start();

    boolean ping = false;
    try {
      ping = latch.await(timeout, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      log.error("Exception making ping to {} : {}", ipAddress, e.getClass());
    }
    if (!ping) {
      t.interrupt();
    }

    return ping;
  }

  public String getTmpFolder() {
    return tmpFolder;
  }

  public String getHost() {
    return host;
  }

  public String getPem() {
    return pem;
  }

  public void setPem(String pem) {
    this.pem = pem;
  }

  public OverthereConnection getConnection() {
    return connection;
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

}
