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
import static org.kurento.test.monitor.SystemMonitor.MONITOR_PORT_DEFAULT;
import static org.kurento.test.monitor.SystemMonitor.MONITOR_PORT_PROP;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
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
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.kurento.client.MediaType;
import org.kurento.client.RTCInboundRTPStreamStats;
import org.kurento.client.RTCOutboundRTPStreamStats;
import org.kurento.client.Stats;
import org.kurento.client.WebRtcEndpoint;
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
	public static final String INBOUND = "serverside_inbound";
	public static final String OUTBOUND = "serverside_outbound";

	private SystemMonitor monitor;
	private SshConnection remoteKms;
	private int monitorPort;
	private long samplingTime = getProperty(DEFAULT_MONITOR_RATE_PROPERTY,
			DEFAULT_MONITOR_RATE_DEFAULT);
	private Map<Long, MonitorResult> infoMap = new TreeMap<>();
	private NumberFormat formatter = new DecimalFormat("#0.00");

	private Thread thread;
	private boolean showLantency = false;
	private int numClients = 0;
	private double currentLatency = 0;
	private int latencyHints = 0;
	private int latencyErrors = 0;

	// TOD so far only a single web page and WebRtcEndpoint is supported
	private WebPage webPage;
	private WebRtcEndpoint webRtcEndpoint;

	public SystemMonitorManager(String kmsHost, String kmsLogin,
			String kmsPem) {
		try {
			monitorPort = getProperty(MONITOR_PORT_PROP, MONITOR_PORT_DEFAULT);
			remoteKms = new SshConnection(kmsHost, kmsLogin, null, kmsPem);
			remoteKms.start();
			remoteKms.createTmpFolder();
			copyMonitorToRemoteKms();
			startRemoteKms();
			monitor = new SystemMonitor();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public SystemMonitorManager() {
		try {
			String wsUri = getProperty(KMS_WS_URI_PROP, KMS_WS_URI_DEFAULT);
			String kmsLogin = getProperty(KURENTO_KMS_LOGIN_PROP);
			String kmsPasswd = getProperty(KURENTO_KMS_PASSWD_PROP);
			String kmsPem = getProperty(KURENTO_KMS_PEM_PROP);
			monitorPort = getProperty(MONITOR_PORT_PROP, MONITOR_PORT_DEFAULT);

			boolean isKmsRemote = !wsUri.contains("localhost")
					&& !wsUri.contains("127.0.0.1");

			if (isKmsRemote) {
				String remoteKmsStr = wsUri.substring(wsUri.indexOf("//") + 2,
						wsUri.lastIndexOf(":"));
				log.info("Using remote KMS at {}", remoteKmsStr);
				remoteKms = new SshConnection(remoteKmsStr, kmsLogin, kmsPasswd,
						kmsPem);
				remoteKms.start();
				remoteKms.createTmpFolder();
				copyMonitorToRemoteKms();
				startRemoteKms();
			} else {
				monitor = new SystemMonitor();
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void copyMonitorToRemoteKms()
			throws IOException, URISyntaxException {
		final String folder = "/org/kurento/test/monitor/";
		final String[] classesName = { "SystemMonitor.class", "NetInfo.class",
				"NetInfo$NetInfoEntry.class", "KmsSystemInfo.class" };

		Path tempDir = Files.createTempDirectory(null);
		File newDir = new File(tempDir + folder);
		newDir.mkdirs();
		String targetFolder = remoteKms.getTmpFolder() + folder;
		remoteKms.mkdirs(targetFolder);

		for (String className : classesName) {
			Path sourceClass = getPathInClasspath(folder + className);

			Path classFileInDisk = Files.createTempFile("", ".class");
			Files.copy(sourceClass, classFileInDisk,
					StandardCopyOption.REPLACE_EXISTING);
			remoteKms.scp(classFileInDisk.toString(), targetFolder + className);
			Files.delete(classFileInDisk);
		}
	}

	private void startRemoteKms() throws IOException {
		remoteKms.execCommand("sh", "-c", "java -cp " + remoteKms.getTmpFolder()
				+ " org.kurento.test.monitor.SystemMonitor " + monitorPort
				+ " > " + remoteKms.getTmpFolder() + "/monitor.log 2>&1");

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

	private Path getPathInClasspath(String resourceName)
			throws IOException, URISyntaxException {
		return getPathInClasspath(this.getClass().getResource(resourceName));
	}

	private Path getPathInClasspath(URL resource)
			throws IOException, URISyntaxException {
		Objects.requireNonNull(resource, "Resource URL cannot be null");
		URI uri = resource.toURI();

		String scheme = uri.getScheme();
		if (scheme.equals("file")) {
			return Paths.get(uri);
		}

		if (!scheme.equals("jar")) {
			throw new IllegalArgumentException(
					"Cannot convert to Path: " + uri);
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

	public void start() {
		final long start = new Date().getTime();
		thread = new Thread() {
			@Override
			public void run() {
				try {
					while (true) {
						MonitorResult info = new MonitorResult();

						// KMS info
						KmsSystemInfo kmsInfo;
						if (remoteKms != null) {
							kmsInfo = (KmsSystemInfo) sendMessage("measureKms");
						} else {
							kmsInfo = (KmsSystemInfo) monitor.measureKms();
						}
						info.setSystemInfo(kmsInfo);

						// Latency
						info.setLatencyHints(latencyHints);
						info.setLatencyErrors(latencyErrors);
						info.setCurrentLatency(currentLatency);

						// RTC stats
						if (webPage != null) {
							info.setClientRtcStats(webPage.getRtcStats());
						}
						if (webRtcEndpoint != null) {
							info.setServerRtcStats(getStats(webRtcEndpoint));
						}

						// Client number
						info.setNumClients(numClients);

						// Save entry in map
						long time = new Date().getTime() - start;
						infoMap.put(time, info);

						// Wait sampling time
						Thread.sleep(samplingTime);
					}
				} catch (InterruptedException ie) {
					log.warn("Interrupted exception in system monitor manager");
				} catch (Exception e) {
					log.error("Exception in system monitor manager", e);
				}
			}
		};
		thread.setDaemon(true);
		thread.start();
	}

	@SuppressWarnings("deprecation")
	public void stop() {
		thread.interrupt();
		thread.stop();
	}

	// TODO clean this method
	public void writeResults(String csvFile) {
		PrintWriter pw;
		try {
			pw = new PrintWriter(new FileWriter(csvFile));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		boolean header = false;
		String emptyStats = "";
		List<String> rtcClientHeader = null;
		List<String> rtcServerHeader = null;

		for (long time : infoMap.keySet()) {
			if (!header) {
				pw.print("time,clients_number,kms_threads_number");
				pw.print(",cpu_percetage,mem_bytes,mem_percentage");
				pw.print(",swap_bytes,swap_percentage");

				if (showLantency) {
					pw.print(",latency_ms_avg,latency_errors_number");
				}
				pw.print(infoMap.get(time).getSystemInfo().getNetInfo()
						.parseHeaderEntry());

				// Browser statistics. First entries may be empty, so we have to
				// iterate to find values in the statistics in order to write
				// the header in the resulting CSV
				if (webPage != null) {
					rtcClientHeader = new ArrayList<>();
					for (MonitorResult info : infoMap.values()) {
						Map<String, Object> clientRtcStats = info
								.getClientRtcStats();
						if (clientRtcStats != null
								&& !clientRtcStats.isEmpty()) {
							for (String rtcStatsKey : clientRtcStats.keySet()) {
								if (!rtcClientHeader.contains(rtcStatsKey)) {
									rtcClientHeader.add(rtcStatsKey);
									pw.print("," + rtcStatsKey);
									emptyStats += ",";
								}
							}
						}
					}
				}
				if (webRtcEndpoint != null) {
					rtcServerHeader = new ArrayList<>();
					for (MonitorResult info : infoMap.values()) {
						Map<String, Stats> serverRtcStats = info
								.getServerRtcStats();
						if (serverRtcStats != null
								&& !serverRtcStats.isEmpty()) {
							for (String rtcStatsKey : serverRtcStats.keySet()) {
								Object object = serverRtcStats.get(rtcStatsKey);
								for (Method method : object.getClass()
										.getMethods()) {
									if (isGetter(method)) {
										String keyList = rtcStatsKey + "_"
												+ getGetterName(method);
										if (!rtcServerHeader
												.contains(keyList)) {
											rtcServerHeader.add(keyList);
											pw.print("," + keyList);
											emptyStats += ",";
										}
									}
								}
							}
						}
					}
				}

				pw.println("");
				header = true;
			}
			String parsedtime = new SimpleDateFormat("mm:ss.SSS").format(time);
			double cpu = infoMap.get(time).getSystemInfo().getCpuPercent();
			long mem = infoMap.get(time).getSystemInfo().getMem();
			double memPercent = infoMap.get(time).getSystemInfo()
					.getMemPercent();
			long swap = infoMap.get(time).getSystemInfo().getSwap();
			double swapPercent = infoMap.get(time).getSystemInfo()
					.getSwapPercent();

			pw.print(parsedtime + "," + infoMap.get(time).getNumClients() + ","
					+ infoMap.get(time).getSystemInfo().getNumThreadsKms() + ","
					+ formatter.format(cpu) + "," + mem + ","
					+ formatter.format(memPercent) + "," + swap + ","
					+ formatter.format(swapPercent));

			if (showLantency) {
				pw.print("," + infoMap.get(time).getLatency() + ","
						+ infoMap.get(time).getLatencyErrors());
			}

			pw.print(infoMap.get(time).getSystemInfo().getNetInfo()
					.parseNetEntry());

			// Browser statistics
			if (webPage != null) {
				Map<String, Object> clientRtcStats = infoMap.get(time)
						.getClientRtcStats();
				if (clientRtcStats != null && !clientRtcStats.isEmpty()) {
					for (String key : rtcClientHeader) {
						pw.print(",");
						if (clientRtcStats.containsKey(key)) {
							pw.print(clientRtcStats.get(key));
						}
					}
				} else {
					pw.print(emptyStats);
				}
			}
			if (webRtcEndpoint != null) {
				Map<String, Object> rtcServerStatsValues = new HashMap<>();

				Map<String, Stats> serverRtcStats = infoMap.get(time)
						.getServerRtcStats();
				if (serverRtcStats != null && !serverRtcStats.isEmpty()) {
					for (String rtcStatsKey : serverRtcStats.keySet()) {
						Object object = serverRtcStats.get(rtcStatsKey);
						for (Method method : object.getClass().getMethods()) {
							if (isGetter(method)) {
								Object value = null;
								try {
									value = method.invoke(object);
								} catch (Exception e) {
									log.error("Exception invoking method", e);
								}

								String keyList = rtcStatsKey + "_"
										+ getGetterName(method);
								rtcServerStatsValues.put(keyList, value);
							}
						}
					}

					for (String rtcHeader : rtcServerHeader) {
						pw.print(",");
						if (rtcServerStatsValues.get(rtcHeader) != null) {
							pw.print(rtcServerStatsValues.get(rtcHeader));
						}
					}

				}
			} else {
				pw.print(emptyStats);
			}

			pw.println("");
		}
		pw.close();

	}

	private boolean isGetter(Method method) {
		return (method.getName().startsWith("get")
				|| method.getName().startsWith("is"))
				&& !method.getName().equals("getClass");
	}

	private String getGetterName(Method method) {
		String name = method.getName();
		if (name.startsWith("get")) {
			name = name.substring(3);
		} else if (name.startsWith("is")) {
			name = name.substring(2);
		}
		return name;
	}

	private Map<String, Stats> getStats(WebRtcEndpoint webRtcEndpoint) {
		Map<String, Stats> stats = new HashMap<>();
		MediaType[] types = { MediaType.VIDEO, MediaType.AUDIO,
				MediaType.DATA };

		for (MediaType type : types) {
			Map<String, Stats> trackStats = webRtcEndpoint.getStats(type);
			for (Stats track : trackStats.values()) {
				stats.put(type.name().toLowerCase() + "_"
						+ getRtcStatsType(track.getClass()), track);
			}
		}

		return stats;
	}

	private String getRtcStatsType(Class<?> clazz) {
		String type = clazz.getSimpleName();
		if (clazz.equals(RTCInboundRTPStreamStats.class)) {
			type = INBOUND;
		} else if (clazz.equals(RTCOutboundRTPStreamStats.class)) {
			type = OUTBOUND;
		}
		return type;
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

	public void setWebPage(WebPage webPage) {
		this.webPage = webPage;
	}

	public void setWebRtcEndpoint(WebRtcEndpoint webRtcEndpoint) {
		this.webRtcEndpoint = webRtcEndpoint;
	}

}
