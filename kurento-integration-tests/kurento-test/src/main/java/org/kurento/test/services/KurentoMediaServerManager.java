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
package org.kurento.test.services;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.kurento.commons.Address;
import org.kurento.commons.PropertiesManager;
import org.kurento.commons.exception.KurentoException;
import org.kurento.test.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;

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
	private static final String KURENTO_SERVER_COMMAND_DEFAULT = "/usr/bin/kurento-media-server";

	private static final String KURENTO_SERVER_DEBUG_PROP = "kms.debug";
	private static final String KURENTO_SERVER_DEBUG_DEFAULT = "2,*media_server*:5,*Kurento*:5,KurentoMediaServerServiceHandler:7";

	public static Logger log = LoggerFactory
			.getLogger(KurentoMediaServerManager.class);

	private static String workspace;

	private int httpPort;
	private String testClassName;
	private String testMethodName;
	private String testDir;
	private String serverCommand;
	private String gstPlugins;
	private String debugOptions;

	private Address rabbitMqAddress;
	private String wsUri;

	public static KurentoMediaServerManager createWithWsTransport(String wsUri,
			int httpPort) {

		KurentoMediaServerManager manager = new KurentoMediaServerManager();
		manager.wsUri = wsUri;
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

	public void setTestDir(String testDir) {
		this.testDir = testDir;
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
			workspace = Files.createTempDirectory("kurento-test").toString();
		} catch (IOException e) {
			workspace = PropertiesManager.getProperty(KURENTO_WORKSPACE_PROP,
					KURENTO_WORKSPACE_DEFAULT);
			log.error(
					"Exception loading temporal folder; instead folder {} will be used",
					workspace, e);
		}

		debugOptions = PropertiesManager.getProperty(KURENTO_SERVER_DEBUG_PROP,
				KURENTO_SERVER_DEBUG_DEFAULT);

		if (!workspace.endsWith("/")) {
			workspace += "/";
		}
		log.debug("Local folder to store temporal files: {}", workspace);

		if (rabbitMqAddress != null) {
			log.info("Starting KMS with RabbitMQ: RabbitMQAddress:'{}'"
					+ " serverCommand:'{}' gstPlugins:'{}' workspace: '{}'",
					rabbitMqAddress, serverCommand, gstPlugins, workspace);
		} else {
			log.info("Starting KMS with Ws uri: '{}'"
					+ " serverCommand:'{}' gstPlugins:'{}' workspace: '{}'",
					wsUri, serverCommand, gstPlugins, workspace);
		}

		createKurentoConf();

		if (testDir != null) {

			File logFile = new File(testDir + testClassName, testMethodName
					+ "-kms.log");
			KurentoServicesTestHelper.setServerLogFilePath(logFile);

			log.debug("Log file: {}", logFile.getAbsolutePath());

			Shell.runAndWait("sh", "-c",
					workspace + "kurento.sh > " + logFile.getAbsolutePath()
							+ " 2>&1");
		} else {

			Shell.run("sh", "-c", workspace + "kurento.sh");
		}

		waitForKurentoMediaServer();
	}

	private void waitForKurentoMediaServer() {

		// TODO Wait until KMS is ready instead of 2s
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			log.error("InterruptedException {}", e.getMessage());
		}
	}

	private void createKurentoConf() {

		Configuration cfg = new Configuration();

		// TODO Create new format config file

		// Data-model
		Map<String, Object> data = new HashMap<String, Object>();

		if (rabbitMqAddress != null) {
			data.put("transport", "rabbitmq");
			data.put("rabbitAddress", rabbitMqAddress.getHost());
			data.put("rabbitPort", String.valueOf(rabbitMqAddress.getPort()));
		} else {

			URI wsAsUri;
			try {
				wsAsUri = new URI(wsUri);
				int port = wsAsUri.getPort();
				String path = wsAsUri.getPath();
				data.put("transport", "ws");
				data.put("wsPort", String.valueOf(port));
				data.put("wsPath", path.substring(1));

			} catch (URISyntaxException e) {
				throw new KurentoException("Invalid ws uri: " + wsUri);
			}
		}

		data.put("gstPlugins", gstPlugins);
		data.put("debugOptions", debugOptions);
		data.put("serverCommand", serverCommand);
		data.put("workspace", workspace);
		data.put("httpEndpointPort", String.valueOf(httpPort));

		cfg.setClassForTemplateLoading(KurentoMediaServerManager.class,
				"/templates/");

		createFileFromTemplate(cfg, data, "kurento.conf.json");
		createFileFromTemplate(cfg, data, "pattern.sdp");
		createFileFromTemplate(cfg, data, "kurento.sh");
		Shell.runAndWait("chmod", "+x", workspace + "kurento.sh");
	}

	private void createFileFromTemplate(Configuration cfg,
			Map<String, Object> data, String filename) {

		try {

			Template template = cfg.getTemplate(filename + ".ftl");
			File file = new File(workspace + filename);
			Writer writer = new FileWriter(file);
			template.process(data, writer);
			writer.flush();
			writer.close();

			log.debug("Created file '" + file.getAbsolutePath() + "'");

		} catch (Exception e) {
			throw new RuntimeException(
					"Exception while creating file from template", e);
		}
	}

	public void destroy() {
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
		log.trace("Sending SIGTERM to KMS process");
		Shell.runAndWait("sh", "-c", "kill `cat " + workspace + "kms-pid`");
	}

	private void kmsSigKill() {
		log.trace("Sending SIGKILL to KMS process");
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

	public String getLocalhostWsUrl() {
		return wsUri;
	}

}
