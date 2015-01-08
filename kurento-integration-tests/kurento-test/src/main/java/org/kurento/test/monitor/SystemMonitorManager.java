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
import static org.kurento.test.monitor.SystemMonitor.MONITOR_PORT_DEFAULT;
import static org.kurento.test.monitor.SystemMonitor.MONITOR_PORT_PROP;
import static org.kurento.test.monitor.SystemMonitor.OUTPUT_CSV;
import static org.kurento.test.services.KurentoMediaServerManager.KURENTO_KMS_LOGIN_PROP;
import static org.kurento.test.services.KurentoMediaServerManager.KURENTO_KMS_PASSWD_PROP;
import static org.kurento.test.services.KurentoServicesTestHelper.KMS_WS_URI_DEFAULT;
import static org.kurento.test.services.KurentoServicesTestHelper.KMS_WS_URI_PROP;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;

import org.kurento.test.services.RemoteHost;
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

	private SystemMonitor monitor;
	private RemoteHost remoteKms;
	private int monitorPort;

	public SystemMonitorManager() throws IOException, URISyntaxException {
		String wsUri = getProperty(KMS_WS_URI_PROP, KMS_WS_URI_DEFAULT);
		String kmsLogin = getProperty(KURENTO_KMS_LOGIN_PROP);
		String kmsPasswd = getProperty(KURENTO_KMS_PASSWD_PROP);
		monitorPort = getProperty(MONITOR_PORT_PROP, MONITOR_PORT_DEFAULT);

		boolean isKmsRemote = !wsUri.contains("localhost")
				&& !wsUri.contains("127.0.0.1");

		if (isKmsRemote) {
			String remoteKmsStr = wsUri.substring(wsUri.indexOf("//") + 2,
					wsUri.lastIndexOf(":"));
			log.info("Using remote KMS at {}", remoteKmsStr);
			remoteKms = new RemoteHost(remoteKmsStr, kmsLogin, kmsPasswd);
			remoteKms.start();
			remoteKms.createTmpFolder();
			copyMonitorToRemoteKms();
			startRemoteKms();
		}
		monitor = new SystemMonitor();
	}

	private void copyMonitorToRemoteKms() throws IOException,
			URISyntaxException {
		final String folder = "/org/kurento/test/monitor/";
		final String[] classesName = { "SystemMonitor.class",
				"SystemMonitor$1.class", /* "SystemMonitor$2.class", */
				"NetInfo.class", "NetInfo$NetInfoEntry.class",
				"SystemInfo.class" };

		Path tempDir = Files.createTempDirectory(null);
		File newDir = new File(tempDir + folder);
		newDir.mkdirs();
		String targetFolder = remoteKms.getTmpFolder() + folder;
		remoteKms.mkdirs(targetFolder);

		for (String className : classesName) {
			Path sourceClass = getPathInClasspath(folder + className);
			remoteKms.scp(sourceClass.toString(), targetFolder + className);
		}
	}

	private void startRemoteKms() throws IOException {
		remoteKms.execCommand("sh", "-c",
				"java -cp " + remoteKms.getTmpFolder()
						+ " org.kurento.test.monitor.SystemMonitor "
						+ monitorPort + " > " + remoteKms.getTmpFolder()
						+ "/monitor.log 2>&1");

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
		if (i == max) {
			throw new RuntimeException("Socket in remote KMS not available");
		}
	}

	private Path getPathInClasspath(String resourceName) throws IOException,
			URISyntaxException {
		return getPathInClasspath(this.getClass().getResource(resourceName));
	}

	private Path getPathInClasspath(URL resource) throws IOException,
			URISyntaxException {
		Objects.requireNonNull(resource, "Resource URL cannot be null");
		URI uri = resource.toURI();

		String scheme = uri.getScheme();
		if (scheme.equals("file")) {
			return Paths.get(uri);
		}

		if (!scheme.equals("jar")) {
			throw new IllegalArgumentException("Cannot convert to Path: " + uri);
		}

		String s = uri.toString();
		int separator = s.indexOf("!/");
		String entryName = s.substring(separator + 2);
		URI fileURI = URI.create(s.substring(0, separator));

		FileSystem fs = null;

		try {
			fs = FileSystems.newFileSystem(fileURI,
					Collections.<String, Object> emptyMap());
		} catch (FileSystemAlreadyExistsException e) {
			fs = FileSystems.getFileSystem(fileURI);
		}

		return fs.getPath(entryName);
	}

	public void start() throws IOException {
		if (remoteKms != null) {
			sendMessage("start");
		} else {
			monitor.start();
		}
	}

	public void writeResults(String csvFile) throws IOException {
		if (remoteKms != null) {
			sendMessage("writeResults " + remoteKms.getTmpFolder());
			remoteKms.getFile(csvFile, remoteKms.getTmpFolder() + OUTPUT_CSV);
		} else {
			monitor.writeResults(csvFile);
		}
	}

	public void stop() throws IOException {
		if (remoteKms != null) {
			sendMessage("stop");
		} else {
			monitor.stop();
		}
	}

	public void incrementNumClients() throws IOException {
		if (remoteKms != null) {
			sendMessage("incrementNumClients");
		} else {
			monitor.incrementNumClients();
		}
	}

	public void decrementNumClients() throws IOException {
		if (remoteKms != null) {
			sendMessage("decrementNumClients");
		} else {
			monitor.decrementNumClients();
		}
	}

	public void addCurrentLatency(long latency) throws IOException {
		if (remoteKms != null) {
			sendMessage("addCurrentLatency " + latency);
		} else {
			monitor.addCurrentLatency(latency);
		}
	}

	public void incrementLatencyErrors() throws IOException {
		if (remoteKms != null) {
			sendMessage("incrementLatencyErrors");
		} else {
			monitor.incrementLatencyErrors();
		}
	}

	public void setSamplingTime(long samplingTime) throws IOException {
		if (remoteKms != null) {
			sendMessage("setSamplingTime " + samplingTime);
		} else {
			monitor.setSamplingTime(samplingTime);
		}
	}

	private void sendMessage(String message) throws IOException {
		// log.debug("Sending message {} to {}", message, remoteKms.getHost());
		Socket client = new Socket(remoteKms.getHost(), monitorPort);
		PrintWriter output = new PrintWriter(client.getOutputStream(), true);
		BufferedReader input = new BufferedReader(new InputStreamReader(
				client.getInputStream()));
		// log.debug("Sending message to remote monitor: {}", message);
		output.println(message);

		String returnedMessage = input.readLine();

		if (returnedMessage != null) {
			// TODO handle errors
			// log.debug("Returned message by remote monitor: {}",
			// returnedMessage);
		}
		output.close();
		input.close();
		client.close();
	}

	public void destroy() throws IOException {
		if (remoteKms != null) {
			sendMessage("destroy");
			remoteKms.stop();
		}
	}

}
