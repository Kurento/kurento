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
package org.kurento.test.monitor;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.TestConfiguration.DEFAULT_MONITOR_RATE_DEFAULT;
import static org.kurento.test.TestConfiguration.DEFAULT_MONITOR_RATE_PROPERTY;
import static org.kurento.test.TestConfiguration.KMS_WS_URI_DEFAULT;
import static org.kurento.test.TestConfiguration.KMS_WS_URI_PROP;
import static org.kurento.test.TestConfiguration.KURENTO_KMS_LOGIN_PROP;
import static org.kurento.test.TestConfiguration.KURENTO_KMS_PASSWD_PROP;
import static org.kurento.test.TestConfiguration.KURENTO_KMS_PEM_PROP;
import static org.kurento.test.monitor.KmsMonitor.MONITOR_PORT_DEFAULT;
import static org.kurento.test.monitor.KmsMonitor.MONITOR_PORT_PROP;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.ClassPath;
import org.kurento.commons.exception.KurentoException;
import org.kurento.test.browser.WebPage;
import org.kurento.test.services.SshConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to handle local or remote system monitor.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class SystemMonitorManager {

	public static Logger log = LoggerFactory
			.getLogger(SystemMonitorManager.class);

	public static final String OUTPUT_CSV = "/kms-monitor.csv";

	private KmsMonitor monitor;
	private SshConnection remoteKms;
	private int monitorPort;
	private long samplingTime = getProperty(DEFAULT_MONITOR_RATE_PROPERTY,
			DEFAULT_MONITOR_RATE_DEFAULT);

	private Thread thread;
	private int numClients = 0;
	private double currentLatency = 0;
	private int latencyHints = 0;
	private int latencyErrors = 0;

	private MonitorSampleRegistrer registrer = new MonitorSampleRegistrer();

	private List<WebRtcClient> clients = new CopyOnWriteArrayList<>();

	public SystemMonitorManager(String kmsHost, String kmsLogin,
			String kmsPem) {
		try {
			monitorPort = getProperty(MONITOR_PORT_PROP, MONITOR_PORT_DEFAULT);
			remoteKms = new SshConnection(kmsHost, kmsLogin, null, kmsPem);
			remoteKms.start();
			remoteKms.createTmpFolder();
			copyMonitorToRemoteKms();
			startRemoteProcessMonitor();
			monitor = new KmsMonitor();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public SystemMonitorManager() {

		try {
			String wsUri = getProperty(KMS_WS_URI_PROP, KMS_WS_URI_DEFAULT);

			boolean isKmsRemote = !wsUri.contains("localhost")
					&& !wsUri.contains("127.0.0.1");

			if (isKmsRemote) {

				String kmsLogin = getProperty(KURENTO_KMS_LOGIN_PROP);
				String kmsPasswd = getProperty(KURENTO_KMS_PASSWD_PROP);
				String kmsPem = getProperty(KURENTO_KMS_PEM_PROP);

				startRemoteMonitor(wsUri, kmsLogin, kmsPasswd, kmsPem);

			} else {
				monitor = new KmsMonitor();
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void startRemoteMonitor(String wsUri, String kmsLogin,
			String kmsPasswd, String kmsPem)
					throws IOException, URISyntaxException {

		monitorPort = getProperty(MONITOR_PORT_PROP, MONITOR_PORT_DEFAULT);

		String remoteKmsStr = wsUri.substring(wsUri.indexOf("//") + 2,
				wsUri.lastIndexOf(":"));

		log.info("Monitoring remote KMS at {}", remoteKmsStr);

		copyMonitor(kmsLogin, kmsPasswd, kmsPem, remoteKmsStr);

		startRemoteProcessMonitor();
	}

	private void copyMonitor(String kmsLogin, String kmsPasswd, String kmsPem,
			String remoteKmsStr) throws IOException, URISyntaxException {
		remoteKms = new SshConnection(remoteKmsStr, kmsLogin, kmsPasswd,
				kmsPem);
		remoteKms.start();
		remoteKms.createTmpFolder();
		copyMonitorToRemoteKms();
	}

	private void copyMonitorToRemoteKms()
			throws IOException, URISyntaxException {

		copyClassesToRemote(new Class<?>[] { KmsMonitor.class, NetInfo.class,
				NetInfo.NetInfoEntry.class, KmsSystemInfo.class });
	}

	private void copyClassesToRemote(final Class<?>[] classesName)
			throws IOException {
		String targetFolder = remoteKms.getTmpFolder();

		for (Class<?> className : classesName) {

			String classFile = "/" + className.getName().replace(".", "/")
					+ ".class";

			Path sourceClass = ClassPath.get(classFile);

			Path classFileInDisk = Files.createTempFile("", ".class");
			Files.copy(sourceClass, classFileInDisk,
					StandardCopyOption.REPLACE_EXISTING);
			remoteKms.mkdirs(
					Paths.get(targetFolder + classFile).getParent().toString());
			remoteKms.scp(classFileInDisk.toString(), targetFolder + classFile);

			Files.delete(classFileInDisk);
		}
	}

	private void startRemoteProcessMonitor() throws IOException {

		remoteKms.execCommand("sh", "-c",
				"java -cp " + remoteKms.getTmpFolder() + " "
						+ KmsMonitor.class.getName() + " " + monitorPort + " > "
						+ remoteKms.getTmpFolder() + "/monitor.log 2>&1");

		boolean connected = waitForReady();

		if (!connected) {
			throw new RuntimeException("Socket in remote KMS not available");
		}
	}

	private boolean waitForReady() throws UnknownHostException, IOException {
		// Wait for 600x100 ms = 60 seconds
		Socket client = null;
		int i = 0;
		final int max = 600;
		for (; i < max; i++) {
			try {
				client = new Socket(remoteKms.getHost(), monitorPort);
				break;
			} catch (ConnectException ce) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		}
		if (client != null) {
			client.close();
		}

		return i != max;
	}

	public void startMonitoring() {

		final long startTime = new Date().getTime();
		thread = new Thread() {
			@Override
			public void run() {
				try {
					while (true) {
						registerSample(startTime);
						Thread.sleep(samplingTime);
					}
				} catch (InterruptedException | KurentoException re) {
					log.warn(
							"Monitoring thread interrupted. Finishing execution");
				} catch (Exception e) {
					log.error("Exception in system monitor manager", e);
				}
			}
		};
		thread.setDaemon(true);
		thread.start();
	}

	private void registerSample(final long start) {

		MonitorSample sample = new MonitorSample();

		// KMS info
		KmsSystemInfo kmsInfo;
		if (remoteKms != null) {
			kmsInfo = (KmsSystemInfo) sendMessage("measureKms");
		} else {
			kmsInfo = (KmsSystemInfo) monitor.measureKms();
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
		long time = new Date().getTime() - start;
		registrer.addSample(time, sample);
	}

	@SuppressWarnings("deprecation")
	public void stop() {
		thread.interrupt();
		try {
			thread.join(3000);
			if (thread.isAlive()) {
				log.warn(
						"Monitoring thread not stopped 3s before interrupted. Force stop");
				thread.stop();
			}
		} catch (InterruptedException e) {
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
			PrintWriter output = new PrintWriter(client.getOutputStream(),
					true);
			ObjectInputStream input = new ObjectInputStream(
					client.getInputStream());
			output.println(message);

			returnedMessage = input.readObject();
			log.debug("Receive message {}", returnedMessage);

			output.close();
			input.close();
			client.close();

		} catch (Exception e) {
			throw new RuntimeException(e);
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

	public void addWebRtcClientAndActivateStats(String id,
			WebRtcEndpoint webRtcEndpoint, WebPage page,
			String peerConnectionId) {
		addWebRtcClientAndActivateInboundStats(id, webRtcEndpoint, page,
				peerConnectionId);
		addWebRtcClientAndActivateOutboundStats(id, webRtcEndpoint, page,
				peerConnectionId);
	}

	public void addWebRtcClientAndActivateOutboundStats(String id,
			WebRtcEndpoint webRtcEndpoint, WebPage page,
			String peerConnectionId) {

		page.activatePeerConnectionOutboundStats(peerConnectionId);

		addWebRtcClient(id, webRtcEndpoint, page);
	}

	public void addWebRtcClientAndActivateInboundStats(String id,
			WebRtcEndpoint webRtcEndpoint, WebPage page,
			String peerConnectionId) {

		page.activatePeerConnectionInboundStats(peerConnectionId);

		addWebRtcClient(id, webRtcEndpoint, page);
	}

	public void addWebRtcClient(String id, WebRtcEndpoint webRtcEndpoint,
			WebPage page) {

		this.clients.add(new WebRtcClient(id, webRtcEndpoint, page));
	}

}
