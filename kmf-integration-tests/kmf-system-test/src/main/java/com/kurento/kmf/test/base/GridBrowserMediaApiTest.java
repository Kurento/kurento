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
package com.kurento.kmf.test.base;

import static com.kurento.kmf.common.PropertiesManager.getProperty;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;

import com.kurento.kmf.commons.tests.SystemMediaApiTests;
import com.kurento.kmf.media.factory.KmfMediaApiProperties;
import com.kurento.kmf.test.Shell;
import com.kurento.kmf.test.services.Node;
import com.kurento.kmf.test.services.RemoteHost;
import com.kurento.kmf.test.services.SeleniumGridHub;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Base for tests using kmf-media-api, Jetty Http Server and Selenium Grid.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
@Category(SystemMediaApiTests.class)
public class GridBrowserMediaApiTest extends BrowserMediaApiTest {

	public static final int DEFAULT_HUB_PORT = 4444;

	private static final int TIMEOUT_NODE = 60; // seconds
	private static final int MAX_INSTANCES = 5; // seconds

	private static final String LAUNCH_SH = "launch-node.sh";
	private static final String REMOTE_FOLDER = "kurento-test";
	private static final String REMOTE_PID_FILE = "node-pid";

	private SeleniumGridHub seleniumGridHub;
	private List<RemoteHost> remoteHostList;
	private String hubAddress;
	private int hubPort;

	public List<Node> nodes;

	@Before
	public void startGrid() throws Exception {
		startHub();
		startNodes();
	}

	private void startHub() throws Exception {
		hubAddress = KmfMediaApiProperties.getThriftKmfAddress().getHost();
		hubPort = getProperty("test.hub.port", DEFAULT_HUB_PORT);

		seleniumGridHub = new SeleniumGridHub(hubAddress, hubPort);
		seleniumGridHub.start();
	}

	private void startNodes() throws IOException {
		remoteHostList = new ArrayList<RemoteHost>();

		for (Node n : nodes) {
			final String chromeDriverSource = getPathTestFiles()
					+ "/bin/chromedriver/2.9/linux64/chromedriver";
			final String seleniumJarName = "/selenium-server-standalone-2.42.2.jar";
			final String seleniumJarSource = getPathTestFiles() + "/jar"
					+ seleniumJarName;

			RemoteHost remoteHost = new RemoteHost(n.getAddress(),
					n.getLogin(), n.getPassword());
			remoteHost.start();

			// OverThere SCP need absolute path, so home path must be known
			String remoteHome = remoteHost.execAndWaitCommandNoBr("echo", "~");
			// FIXME: This command sometimes fails due to an OverThere exception

			final String remoteFolder = remoteHome + "/" + REMOTE_FOLDER;
			final String remoteChromeDriver = remoteFolder + "/chromedriver";
			final String remoteSeleniumJar = remoteFolder + seleniumJarName;
			final String remoteScript = remoteFolder + "/" + LAUNCH_SH;
			final String remotePort = String.valueOf(remoteHost.getFreePort());

			if (!remoteHost.exists(remoteFolder) || n.isOverwrite()) {
				remoteHost.execAndWaitCommand("mkdir", "-p", remoteFolder);
			}
			if (!remoteHost.exists(remoteChromeDriver) || n.isOverwrite()) {
				remoteHost.scp(chromeDriverSource, remoteChromeDriver);
				remoteHost
						.execAndWaitCommand("chmod", "+x", remoteChromeDriver);
			}
			if (!remoteHost.exists(remoteSeleniumJar) || n.isOverwrite()) {
				remoteHost.scp(seleniumJarSource, remoteSeleniumJar);
			}

			// Script is always overwritten
			// createRemoteScript(remoteHost, remotePort, remoteScript,
			// remoteFolder, remoteChromeDriver, remoteSeleniumJar,
			// n.getBrowser());

			// Launch node
			// remoteHost.execCommand(remoteScript);

			remoteHost.execCommand(
					"xvfb-run",
					"java",
					"-jar",
					remoteSeleniumJar,
					"-port",
					remotePort,
					"-role",
					"node",
					"-hub",
					"http://" + hubAddress + ":" + hubPort + "/grid/register",
					"-browser",
					"browserName=" + n.getBrowser() + ",maxInstances="
							+ n.getMaxInstances(), "-Dwebdriver.chrome.driver="
							+ remoteChromeDriver, "-timeout", "0");

			// Wait to be available for Hub
			waitForNode(n.getAddress(), remotePort);

			remoteHostList.add(remoteHost);
		}
	}

	// FIXME check this method or remove
	private void createRemoteScript(RemoteHost remoteHost, String remotePort,
			String remoteScript, String remoteFolder,
			String remoteChromeDriver, String remoteSeleniumJar, String browser)
			throws IOException {

		// Create script for Node
		Configuration cfg = new Configuration();

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("remotePort", String.valueOf(remotePort));
		data.put("maxInstances", String.valueOf(MAX_INSTANCES));
		data.put("hubIp", hubAddress);
		data.put("hubPort", String.valueOf(hubPort));
		data.put("remoteFolder", remoteFolder);
		data.put("remoteChromeDriver", remoteChromeDriver);
		data.put("remoteSeleniumJar", remoteSeleniumJar);
		data.put("pidFile", REMOTE_PID_FILE);
		data.put("browser", browser);

		cfg.setClassForTemplateLoading(GridBrowserMediaApiTest.class,
				"/templates/");

		try {
			Template template = cfg.getTemplate(LAUNCH_SH + ".ftl");
			Writer writer = new FileWriter(new File(LAUNCH_SH));
			template.process(data, writer);
			writer.flush();
			writer.close();

		} catch (Exception e) {
			throw new RuntimeException(
					"Exception while creating file from template", e);
		}

		// Copy script to remote node
		remoteHost.scp(LAUNCH_SH, remoteScript);
		remoteHost.execAndWaitCommand("chmod", "+x", remoteScript);
		Shell.run("rm", LAUNCH_SH);
	}

	private void waitForNode(String node, String port) {
		int responseStatusCode = 0;
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet httpGet = new HttpGet("http://" + node + ":" + port
				+ "/wd/hub/static/resource/hub.html");

		// Wait for a max of TIMEOUT_NODE seconds
		long maxSystemTime = System.currentTimeMillis() + TIMEOUT_NODE * 1000;
		do {
			try {
				HttpResponse response = client.execute(httpGet);
				responseStatusCode = response.getStatusLine().getStatusCode();
			} catch (Exception e) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException ie) {
				}
				if (System.currentTimeMillis() > maxSystemTime) {
					throw new RuntimeException("Timeout (" + TIMEOUT_NODE
							+ " sec) waiting for node");
				}
			}
		} while (responseStatusCode != 200);

		log.info("Node responseStatus " + responseStatusCode);
	}

	@After
	public void stopGrid() throws Exception {
		// Stop Hub
		seleniumGridHub.stop();

		// FIXME : Despite the fact we know the PID, the problem is that there
		// are two different process (/bin/sh /usr/bin/xvfb-run java -jar ...
		// and java -jar selenium-server-standalone-2.42.2.jar)
		// String remotePid = remoteHost.execAndWaitCommandNoBr("cat",
		// REMOTE_FOLDER + "/" + REMOTE_PID_FILE);
		// remoteHost.execCommand("kill", remotePid);
		// remoteHost.execCommand("rm", REMOTE_FOLDER + "/" + REMOTE_PID_FILE);

		for (RemoteHost rh : remoteHostList) {
			// Stop Nodes
			rh.execCommand("killall", "java");

			// Close connection with remote host
			rh.stop();
		}
	}
}
