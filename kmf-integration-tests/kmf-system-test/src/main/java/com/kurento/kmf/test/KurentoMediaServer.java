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
package com.kurento.kmf.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Initializer/stopper class for Kurento Media Server (KMS).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 */
public class KurentoMediaServer {

	public static Logger log = LoggerFactory
			.getLogger(KurentoMediaServer.class);

	private final String serverCommand;
	private final String gstPlugins;
	private String workspace;
	private String debugOptions;
	private final String serverAddress;
	private final int serverPort;
	private final int httpEndpointPort;

	public KurentoMediaServer(String serverAddress, int serverPort,
			int httpEndpointPort) {
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.httpEndpointPort = httpEndpointPort;

		serverCommand = PropertiesManager.getSystemProperty(
				"kurento.serverCommand", "/usr/bin/kurento");
		gstPlugins = PropertiesManager.getSystemProperty("kurento.gstPlugins",
				"");
		workspace = PropertiesManager.getSystemProperty("kurento.workspace",
				"/tmp");
		if (!workspace.endsWith("/")) {
			workspace += "/";
		}

		log.info(
				"KMS config:\n \tserverAddress {}\n \tserverPort {}\n \tserverCommand {}\n \tgstPlugins {}\n \tworkspace {}",
				serverAddress, serverPort, serverCommand, gstPlugins, workspace);

		// Default debug options
		debugOptions = "2,*media_server*:5,*Kurento*:5,KurentoMediaServerServiceHandler:7";
	}

	public boolean isConfigAvailable() {
		return serverCommand != null && gstPlugins != null && workspace != null;
	}

	public void start(String callerTest) throws IOException, TemplateException,
			InterruptedException {

		log.info("Starting KMS in {}:{}, with httpEP port {}", serverAddress,
				serverPort, httpEndpointPort);

		createKurentoConf();
		createFolder(callerTest);

		Shell.run("sh", "-c", workspace + "kurento.sh > " + workspace
				+ callerTest + "/kms.log 2>&1");

		// Guard time to start KMS
		Thread.sleep(3000);
	}

	private void createFolder(String callerTest) {
		File testFolder = new File(workspace + callerTest);
		if (!testFolder.exists()) {
			testFolder.mkdir();
		}
	}

	private void createKurentoConf() throws IOException, TemplateException {
		Configuration cfg = new Configuration();

		// Data-model
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("gstPlugins", gstPlugins);
		data.put("debugOptions", debugOptions);
		data.put("serverCommand", serverCommand);
		data.put("serverAddress", serverAddress);
		data.put("serverPort", serverPort);
		data.put("workspace", workspace);
		data.put("httpEndpointPort", httpEndpointPort);

		cfg.setClassForTemplateLoading(KurentoMediaServer.class, "/templates/");

		createFileFromTemplate(cfg, data, "kurento.conf");
		createFileFromTemplate(cfg, data, "kurento.sh");
		Shell.run("chmod", "+x", workspace + "kurento.sh");
		createFileFromTemplate(cfg, data, "pattern.sdp");
	}

	private void createFileFromTemplate(Configuration cfg,
			Map<String, Object> data, String filename) throws IOException,
			TemplateException {
		Template template = cfg.getTemplate(filename + ".ftl");
		Writer writer = new FileWriter(new File(workspace + filename));
		template.process(data, writer);
		writer.flush();
		writer.close();
	}

	public void stop() {
		Shell.run("killall", "-9", "kurento");
	}

	public String getDebugOptions() {
		return debugOptions;
	}

	public void setDebugOptions(String debugOptions) {
		this.debugOptions = debugOptions;
	}

}
