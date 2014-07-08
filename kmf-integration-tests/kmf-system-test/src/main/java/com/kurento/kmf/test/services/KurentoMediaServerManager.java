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
package com.kurento.kmf.test.services;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;
import com.kurento.kmf.common.Address;
import com.kurento.kmf.common.PropertiesManager;
import com.kurento.kmf.test.Shell;
import com.kurento.kmf.thrift.ThriftInterfaceConfiguration;
import com.kurento.kmf.thrift.pool.ClientPoolException;
import com.kurento.kmf.thrift.pool.ThriftClientPoolService;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Initializer/stopper class for Kurento Media Server (KMS).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class KurentoMediaServerManager {

	private static final String KURENTO_WORKSPACE_PROP = "kurento.workspace";
	private static final String KURENTO_WORKSPACE_DEFAULT = "/tmp";

	private static final String KURENTO_GST_PLUGINS_PROP = "kms.gst.plugins";
	private static final String KURENTO_GST_PLUGINS_DEFAULT = "";

	private static final String KURENTO_SERVER_COMMAND_PROP = "kms.command";
	private static final String KURENTO_SERVER_COMMAND_DEFAULT = "/usr/bin/kurento";

	private static final String KURENTO_SERVER_DEBUG_PROP = "kms.debug";
	private static final String KURENTO_SERVER_DEBUG_DEFAULT = "2,*media_server*:5,*Kurento*:5,KurentoMediaServerServiceHandler:7";

	public static Logger log = LoggerFactory
			.getLogger(KurentoMediaServerManager.class);

	private static String workspace;

	private Address thriftAddress;
	private int httpPort;
	private String testClassName;
	private String testMethodName;
	private String testDir;
	private String serverCommand;
	private String gstPlugins;
	private String debugOptions;

	private Address rabbitMqAddress;

	public static KurentoMediaServerManager createWithThriftTransport(
			Address thriftAddress, int httpPort) {
		KurentoMediaServerManager manager = new KurentoMediaServerManager();
		manager.thriftAddress = thriftAddress;
		manager.httpPort = httpPort;
		return manager;
	}

	public static KurentoMediaServerManager createWithRabbitMqTransport(
			Address rabbitMqAddress, int httpPort) {
		KurentoMediaServerManager manager = new KurentoMediaServerManager();
		manager.rabbitMqAddress = rabbitMqAddress;
		manager.httpPort = httpPort;
		return manager;
	}

	private KurentoMediaServerManager() {
	}

	public void setTestClassName(String testClassName) {
		this.testClassName = testClassName;
	}

	public void setTestMethodName(String testMethodName) {
		this.testMethodName = testMethodName;
	}

	public void start() {

		serverCommand = PropertiesManager.getProperty(
				KURENTO_SERVER_COMMAND_PROP, KURENTO_SERVER_COMMAND_DEFAULT);

		gstPlugins = PropertiesManager.getProperty(KURENTO_GST_PLUGINS_PROP,
				KURENTO_GST_PLUGINS_DEFAULT);

		try {
			workspace = Files.createTempDirectory("kmf-system-test").toString();
		} catch (IOException e) {
			workspace = PropertiesManager.getProperty(KURENTO_WORKSPACE_PROP,
					KURENTO_WORKSPACE_DEFAULT);
			log.error(
					"Exception loading temporal folder; instead folder {} will be used",
					workspace, e);
		}

		debugOptions = PropertiesManager.getProperty(KURENTO_SERVER_DEBUG_PROP,
				KURENTO_SERVER_DEBUG_DEFAULT);

		testDir = "./target/surefire-reports/";

		if (!workspace.endsWith("/")) {
			workspace += "/";
		}
		log.debug("Folder to store temporal files: {}", workspace);

		KurentoServicesTestHelper.setTestDir(testDir);

		if (rabbitMqAddress != null) {
			log.info("Starting KMS with RabbitMQ: RabbitMQAddress:'{}'"
					+ " serverCommand:'{}' gstPlugins:'{}' workspace: '{}'",
					rabbitMqAddress, serverCommand, gstPlugins, workspace);
		} else {
			log.info("Starting KMS with Thrift: thriftAddress:'{}'"
					+ " serverCommand:'{}' gstPlugins:'{}' workspace: '{}'",
					thriftAddress, serverCommand, gstPlugins, workspace);
		}

		createKurentoConf();
		String logFolder = testDir + "TEST-" + testClassName;
		createFolder(logFolder);

		log.debug("Log file: {}", logFolder);

		Shell.runAndWait("sh", "-c", workspace + "kurento.sh > " + logFolder
				+ "/" + testMethodName + "-kms.log 2>&1");

		waitForKurentoMediaServer();
	}

	private void waitForKurentoMediaServer() {

		// Only wait if KMS uses Thrift
		if (thriftAddress != null) {

			long startWaiting = System.currentTimeMillis();

			ThriftClientPoolService clientPool = new ThriftClientPoolService(
					new ThriftInterfaceConfiguration(thriftAddress.getHost(),
							thriftAddress.getPort()));

			// Wait for a max of 20 seconds
			long timeout = System.currentTimeMillis() + 20000;
			while (true) {
				try {
					clientPool.acquireSync();
					break;
				} catch (ClientPoolException e) {
					try {
						Thread.sleep(100);
						if (System.currentTimeMillis() > timeout) {
							throw new RuntimeException(
									"Timeout (20 sec) waiting for ThriftClientPoolService");
						}
					} catch (InterruptedException e1) {
					}
				}
			}

			long waitingTime = System.currentTimeMillis() - startWaiting;

			log.info("KMS started in {} millis", waitingTime);

		} else {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				log.error("InterruptedException {}", e.getMessage());
			}
		}
	}

	private void createFolder(String folder) {
		File folderFile = new File(folder);
		if (!folderFile.exists()) {
			folderFile.mkdirs();
		}
	}

	private void createKurentoConf() {

		Configuration cfg = new Configuration();

		// Data-model
		Map<String, Object> data = new HashMap<String, Object>();

		if (rabbitMqAddress != null) {
			data.put("transport", "RabbitMQ");
			data.put("serverAddress", rabbitMqAddress.getHost());
			data.put("serverPort", String.valueOf(rabbitMqAddress.getPort()));
		} else {
			data.put("transport", "Thrift");
			data.put("serverAddress", thriftAddress.getHost());
			data.put("serverPort", String.valueOf(thriftAddress.getPort()));
		}

		data.put("gstPlugins", gstPlugins);
		data.put("debugOptions", debugOptions);
		data.put("serverCommand", serverCommand);
		data.put("workspace", workspace);
		data.put("httpEndpointPort", String.valueOf(httpPort));

		cfg.setClassForTemplateLoading(KurentoMediaServerManager.class,
				"/templates/");

		createFileFromTemplate(cfg, data, "kurento.conf");
		createFileFromTemplate(cfg, data, "pattern.sdp");
		createFileFromTemplate(cfg, data, "kurento.sh");
		Shell.runAndWait("chmod", "+x", workspace + "kurento.sh");
	}

	private void createFileFromTemplate(Configuration cfg,
			Map<String, Object> data, String filename) {

		try {

			Template template = cfg.getTemplate(filename + ".ftl");
			Writer writer = new FileWriter(new File(workspace + filename));
			template.process(data, writer);
			writer.flush();
			writer.close();

		} catch (Exception e) {
			throw new RuntimeException(
					"Exception while creating file from template", e);
		}
	}

	public void stop() {
		int numKmsProcesses = 0;
		// Max timeout waiting kms ending: 5 seconds
		long timeout = System.currentTimeMillis() + 5000;
		do {
			// If timeout, break the loop
			if (System.currentTimeMillis() > timeout) {
				break;
			}

			// Sending SIGTERM signal to KMS process
			kmsSigTerm();

			// Wait 100 msec to order kms termination
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			numKmsProcesses = countKmsProcesses();

		} while (numKmsProcesses > 0);

		if (numKmsProcesses > 0) {
			// If at this point there is still kms process (after trying to
			// kill it with SIGTERM during 5 seconds), we send the SIGKILL
			// signal to the process
			kmsSigKill();
		}
	}

	private void kmsSigTerm() {
		log.debug("Sending SIGTERM to KMS process");
		Shell.runAndWait("sh", "-c", "kill `cat " + workspace + "kms-pid`");
	}

	private void kmsSigKill() {
		log.debug("Sending SIGKILL to KMS process");
		Shell.runAndWait("sh", "-c", "kill -9 `cat " + workspace + "kms-pid`");
	}

	public String getDebugOptions() {
		return debugOptions;
	}

	public void setDebugOptions(String debugOptions) {
		this.debugOptions = debugOptions;
	}

	public void setHttpPort(int httpPort) {
		this.httpPort = httpPort;
	}

	public int countKmsProcesses() {
		int result = 0;
		try {
			// This command counts number of process (given its PID, stored in
			// kms-pid file)
			String[] command = {
					"sh",
					"-c",
					"ps --pid `cat " + workspace
							+ "kms-pid` --no-headers | wc -l" };
			Process countKms = Runtime.getRuntime().exec(command);

			String stringFromStream = CharStreams
					.toString(new InputStreamReader(countKms.getInputStream(),
							"UTF-8"));
			result = Integer.parseInt(stringFromStream.trim());
		} catch (IOException e) {
			log.error("Exception counting KMS processes", e);
		}

		return result;
	}

	public static String getWorkspace() {
		return workspace;
	}
}
