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
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.common.Address;
import com.kurento.kmf.common.PropertiesManager;
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

	private static final String KURENTO_GST_PLUGINS_PROP = "kurento.gstPlugins";
	private static final String KURENTO_GST_PLUGINS_DEFAULT = "";

	private static final String KURENTO_SERVER_COMMAND_PROP = "kurento.serverCommand";
	private static final String KURENTO_SERVER_COMMAND_DEFAULT = "/usr/bin/kurento";

	public static Logger log = LoggerFactory
			.getLogger(KurentoMediaServerManager.class);

	private Address thriftAddress;
	private int httpPort;

	private String logFolder = "";

	private String serverCommand;
	private String gstPlugins;
	private String workspace;
	private String debugOptions;

	private Process kmsProcess;
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

	public void setLogFolder(String logFolder) {
		this.logFolder = logFolder;
	}

	private KurentoMediaServerManager() {
	}

	public void start() {

		serverCommand = PropertiesManager.getProperty(
				KURENTO_SERVER_COMMAND_PROP, KURENTO_SERVER_COMMAND_DEFAULT);

		gstPlugins = PropertiesManager.getProperty(KURENTO_GST_PLUGINS_PROP,
				KURENTO_GST_PLUGINS_DEFAULT);

		workspace = PropertiesManager.getProperty(KURENTO_WORKSPACE_PROP,
				KURENTO_WORKSPACE_DEFAULT);

		if (!workspace.endsWith("/")) {
			workspace += "/";
		}

		// Default debug options
		debugOptions = "2,*media_server*:5,*Kurento*:5,KurentoMediaServerServiceHandler:7";

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
		createFolder(logFolder);

		String outputLog = workspace + logFolder + "/kms.log";

		log.debug("Log file: {}", outputLog);

		String[] kmsCommand = { serverCommand, "-f", workspace + "kurento.conf" };
		launchKms(outputLog, kmsCommand);

		waitForKurentoMediaServer();
	}

	private void waitForKurentoMediaServer() {

		// Only wait if KMS uses Thrift
		if (thriftAddress != null) {

			long startWaiting = System.currentTimeMillis();

			ThriftClientPoolService clientPool = new ThriftClientPoolService(
					new ThriftInterfaceConfiguration(thriftAddress.getHost(),
							thriftAddress.getPort()));

			// FIXME Don't wait forever...
			while (true) {
				try {
					clientPool.acquireSync();
					break;
				} catch (ClientPoolException e) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
					}
				}
			}

			long waitingTime = System.currentTimeMillis() - startWaiting;

			log.info("KMS started in {} millis", waitingTime);

		}
	}

	private void createFolder(String callerTest) {
		File testFolder = new File(workspace + callerTest);
		if (!testFolder.exists()) {
			testFolder.mkdir();
		}
	}

	private void createKurentoConf() {

		Configuration cfg = new Configuration();

		// Data-model
		Map<String, Object> data = new HashMap<String, Object>();

		if (rabbitMqAddress != null) {
			data.put("transport", "RabbitMQ");
			data.put("serverAddress", rabbitMqAddress.getHost());
			data.put("serverPort", rabbitMqAddress.getPort());
		} else {
			data.put("transport", "Thrift");
			data.put("serverAddress", thriftAddress.getHost());
			data.put("serverPort", thriftAddress.getPort());
		}

		data.put("serverCommand", serverCommand);
		data.put("workspace", workspace);
		data.put("httpEndpointPort", httpPort);

		cfg.setClassForTemplateLoading(KurentoMediaServerManager.class,
				"/templates/");

		createFileFromTemplate(cfg, data, "kurento.conf");
		createFileFromTemplate(cfg, data, "pattern.sdp");
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
		kmsProcess.destroy();

		// FIXME Improve termination
		// Shell.run("sh", "-c", "killall -9 kurento");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public String getDebugOptions() {
		return debugOptions;
	}

	public void setDebugOptions(String debugOptions) {
		this.debugOptions = debugOptions;
	}

	public void launchKms(final String outputLog, final String... command) {
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					ProcessBuilder builder = new ProcessBuilder(command);
					builder.redirectOutput(new File(outputLog));
					builder.redirectError(new File(outputLog));
					builder.environment().put("GST_PLUGIN_PATH", gstPlugins);
					builder.environment().put("GST_DEBUG", getDebugOptions());
					kmsProcess = builder.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

	public void setHttpPort(int httpPort) {
		this.httpPort = httpPort;
	}
}
