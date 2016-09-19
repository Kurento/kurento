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

package org.kurento.test.monitor;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.config.TestConfiguration.DEFAULT_MONITOR_RATE_DEFAULT;
import static org.kurento.test.config.TestConfiguration.DEFAULT_MONITOR_RATE_PROPERTY;
import static org.kurento.test.config.TestConfiguration.KMS_LOGIN_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_PASSWD_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_PEM_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_SCOPE_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_SCOPE_PROP;
import static org.kurento.test.config.TestConfiguration.KMS_WS_URI_DEFAULT;
import static org.kurento.test.config.TestConfiguration.KMS_WS_URI_PROP;
import static org.kurento.test.monitor.KmsLocalMonitor.MONITOR_PORT_DEFAULT;
import static org.kurento.test.monitor.KmsLocalMonitor.MONITOR_PORT_PROP;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.ClassPath;
import org.kurento.commons.exception.KurentoException;
import org.kurento.commons.net.RemoteService;
import org.kurento.test.browser.WebPage;
import org.kurento.test.services.KmsService;
import org.kurento.test.utils.SshConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to handle local or remote system monitor.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class SystemMonitorManager {

  public static Logger log = LoggerFactory.getLogger(SystemMonitorManager.class);

  public static final String OUTPUT_CSV = "/kms-monitor.csv";

  private KmsMonitor monitor;
  private SshConnection remoteKms;
  private int monitorPort;
  private long samplingTime = getProperty(DEFAULT_MONITOR_RATE_PROPERTY,
      DEFAULT_MONITOR_RATE_DEFAULT);

  private Thread thread;
  private ExecutorService executor;
  private int numClients = 0;
  private double currentLatency = 0;
  private int latencyHints = 0;
  private int latencyErrors = 0;

  private MonitorSampleRegistrer registrer = new MonitorSampleRegistrer();

  private List<WebRtcClient> clients = new CopyOnWriteArrayList<>();

  public SystemMonitorManager(String kmsHost, String kmsLogin, String kmsPem) {
    try {
      monitorPort = getProperty(MONITOR_PORT_PROP, MONITOR_PORT_DEFAULT);
      remoteKms = new SshConnection(kmsHost, kmsLogin, null, kmsPem);
      remoteKms.start();
      remoteKms.createTmpFolder();
      copyMonitorToRemoteKms();
      startRemoteProcessMonitor();
      monitor = new KmsLocalMonitor();
    } catch (Exception e) {
      throw new KurentoException(e);
    }
  }

  public SystemMonitorManager() {

    try {
      String wsUri = getProperty(KMS_WS_URI_PROP, KMS_WS_URI_DEFAULT);
      String kmsScope = getProperty(KMS_SCOPE_PROP, KMS_SCOPE_DEFAULT);

      boolean isKmsRemote = !wsUri.contains("localhost") && !wsUri.contains("127.0.0.1");
      boolean isKmsDocker = kmsScope.equalsIgnoreCase("docker");

      if (isKmsDocker) {
        // "Dockerized" KMS
        String containerId = KmsService.getMonitoredDockerContainerName();
        log.debug("KMS container ID: {}", containerId);
        monitor = new KmsDockerMonitor(containerId);

      } else if (isKmsRemote) {
        // Remote KMS

        String kmsLogin = getProperty(KMS_LOGIN_PROP);
        String kmsPasswd = getProperty(KMS_PASSWD_PROP);
        String kmsPem = getProperty(KMS_PEM_PROP);

        startRemoteMonitor(wsUri, kmsLogin, kmsPasswd, kmsPem);

      } else {
        // Local KMS

        monitor = new KmsLocalMonitor();
      }

    } catch (Exception e) {
      throw new KurentoException(e);
    }
  }

  private void startRemoteMonitor(String wsUri, String kmsLogin, String kmsPasswd, String kmsPem)
      throws IOException, URISyntaxException {

    monitorPort = getProperty(MONITOR_PORT_PROP, MONITOR_PORT_DEFAULT);

    String remoteKmsStr = wsUri.substring(wsUri.indexOf("//") + 2, wsUri.lastIndexOf(":"));

    log.debug("Monitoring remote KMS at {}", remoteKmsStr);

    copyMonitor(kmsLogin, kmsPasswd, kmsPem, remoteKmsStr);

    startRemoteProcessMonitor();
  }

  private void copyMonitor(String kmsLogin, String kmsPasswd, String kmsPem, String remoteKmsStr)
      throws IOException, URISyntaxException {
    remoteKms = new SshConnection(remoteKmsStr, kmsLogin, kmsPasswd, kmsPem);
    remoteKms.start();
    remoteKms.createTmpFolder();
    copyMonitorToRemoteKms();
  }

  private void copyMonitorToRemoteKms() throws IOException, URISyntaxException {

    copyClassesToRemote(new Class<?>[] { KmsMonitor.class, KmsLocalMonitor.class, NetInfo.class,
        NetInfo.NetInfoEntry.class, KmsSystemInfo.class });
  }

  private void copyClassesToRemote(final Class<?>[] classesName) throws IOException {
    String targetFolder = remoteKms.getTmpFolder();

    for (Class<?> className : classesName) {

      String classFile = "/" + className.getName().replace(".", "/") + ".class";

      Path sourceClass = ClassPath.get(classFile);

      Path classFileInDisk = Files.createTempFile("", ".class");
      Files.copy(sourceClass, classFileInDisk, StandardCopyOption.REPLACE_EXISTING);
      remoteKms.mkdirs(Paths.get(targetFolder + classFile).getParent().toString());
      remoteKms.scp(classFileInDisk.toString(), targetFolder + classFile);

      Files.delete(classFileInDisk);
    }
  }

  private void startRemoteProcessMonitor() throws IOException {

    remoteKms.execCommand("sh", "-c",
        "java -cp " + remoteKms.getTmpFolder() + " " + KmsLocalMonitor.class.getName() + " "
            + monitorPort + " > " + remoteKms.getTmpFolder() + "/monitor.log 2>&1");

    try {
      RemoteService.waitForReady(remoteKms.getHost(), monitorPort, 60, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      throw new KurentoException("Monitor in remote KMS is not available");
    }
  }

  public void startMonitoring() {

    final long startTime = new Date().getTime();
    executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    thread = new Thread() {
      @Override
      public void run() {
        try {
          while (true) {
            executor.execute(new Runnable() {
              @Override
              public void run() {
                registerSample(startTime);
              }
            });

            Thread.sleep(samplingTime);
          }
        } catch (InterruptedException | KurentoException re) {
          log.warn("Monitoring thread interrupted. Finishing execution");
        } catch (Exception e) {
          log.error("Exception in system monitor manager", e);
        }
      }
    };
    thread.setDaemon(true);
    thread.start();
  }

  private void registerSample(final long start) {
    long time = new Date().getTime() - start;
    MonitorSample sample = new MonitorSample();

    // KMS info
    KmsSystemInfo kmsInfo;
    if (remoteKms != null) {
      kmsInfo = (KmsSystemInfo) sendMessage("measureKms");
    } else {
      kmsInfo = monitor.measureKms();
    }
    sample.setSystemInfo(kmsInfo);

    // Latency
    sample.setLatencyHints(latencyHints);
    sample.setLatencyErrors(latencyErrors);
    sample.setCurrentLatency(currentLatency);

    // RTC stats
    for (WebRtcClient client : clients) {
      sample.addWebRtcStats(client.getWebRtcStats());
    }

    // Client number
    sample.setNumClients(numClients);

    // Save entry in map
    registrer.addSample(time, sample);
  }

  @SuppressWarnings("deprecation")
  public void stop() {
    executor.shutdown();
    thread.interrupt();
    try {
      thread.join(3000);
      if (thread.isAlive()) {
        log.warn("Monitoring thread not stopped 3s before interrupted. Force stop");
        thread.stop();
      }
    } catch (InterruptedException e) { // Intentionally left blank
    }
  }

  public void writeResults(String csvFile) throws IOException {
    registrer.writeResults(csvFile);
  }

  private Object sendMessage(String message) {
    Object returnedMessage = null;
    try {
      log.debug("Sending message {} to {}", message, remoteKms.getHost());
      Socket client = new Socket(remoteKms.getHost(), monitorPort);
      PrintWriter output = new PrintWriter(client.getOutputStream(), true);
      ObjectInputStream input = new ObjectInputStream(client.getInputStream());
      output.println(message);

      returnedMessage = input.readObject();
      log.debug("Receive message {}", returnedMessage);

      output.close();
      input.close();
      client.close();

    } catch (Exception e) {
      throw new KurentoException(e);
    }

    return returnedMessage;
  }

  public void destroy() {
    if (remoteKms != null) {
      sendMessage("destroy");
      remoteKms.stop();
    }
  }

  public void setSamplingTime(long samplingTime) {
    this.samplingTime = samplingTime;
  }

  public long getSamplingTime() {
    return samplingTime;
  }

  public synchronized void incrementNumClients() {
    this.numClients++;
  }

  public synchronized void decrementNumClients() {
    this.numClients--;
  }

  public synchronized void incrementLatencyErrors() {
    this.latencyErrors++;
  }

  public synchronized void addCurrentLatency(double currentLatency) {
    this.currentLatency += currentLatency;
    this.latencyHints++;
  }

  public void addWebRtcClientAndActivateStats(String id, WebRtcEndpoint webRtcEndpoint,
      WebPage page, String peerConnectionId) {
    addWebRtcClientAndActivateInboundStats(id, webRtcEndpoint, page, peerConnectionId);
    addWebRtcClientAndActivateOutboundStats(id, webRtcEndpoint, page, peerConnectionId);
  }

  public void addWebRtcClientAndActivateOutboundStats(String id, WebRtcEndpoint webRtcEndpoint,
      WebPage page, String peerConnectionId) {

    page.activatePeerConnectionOutboundStats(peerConnectionId);

    addWebRtcClient(id, webRtcEndpoint, page);
  }

  public void addWebRtcClientAndActivateInboundStats(String id, WebRtcEndpoint webRtcEndpoint,
      WebPage page, String peerConnectionId) {

    page.activatePeerConnectionInboundStats(peerConnectionId);

    addWebRtcClient(id, webRtcEndpoint, page);
  }

  public void addWebRtcClient(String id, WebRtcEndpoint webRtcEndpoint, WebPage page) {

    this.clients.add(new WebRtcClient(id, webRtcEndpoint, page));
  }

  public void setShowLantency(boolean showLantency) {
    registrer.setShowLantency(showLantency);
  }

}
