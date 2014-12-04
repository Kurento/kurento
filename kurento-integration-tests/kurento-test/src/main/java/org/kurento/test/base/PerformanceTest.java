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
package org.kurento.test.base;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.kurento.commons.testing.SystemPerformanceTests;
import org.kurento.test.Shell;
import org.kurento.test.client.Browser;
import org.kurento.test.services.Node;
import org.kurento.test.services.Randomizer;
import org.kurento.test.services.RemoteHost;
import org.kurento.test.services.SeleniumGridHub;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Base for tests using kurento-client, Jetty Http Server and Selenium Grid.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.5
 */
@Category(SystemPerformanceTests.class)
public class PerformanceTest extends BrowserKurentoClientTest {

	public static final String SELENIUM_HUB_PORT_PROPERTY = "selenium.hub.port";
	public static final int SELENIUM_HUB_PORT_DEFAULT = 4444;

	public static final String SELENIUM_HUB_HOST_PROPERTY = "selenium.hub.host";
	public static final String SELENIUM_HUB_HOST_DEFAULT = "127.0.0.1";

	private static final int TIMEOUT_NODE = 300; // seconds
	private static final String LAUNCH_SH = "launch-node.sh";

	private SeleniumGridHub seleniumGridHub;
	private String hubAddress;
	private int hubPort;
	private CountDownLatch countDownLatch;

	public List<Node> nodes;

	@Before
	public void startGrid() throws Exception {
		startHub();
		startNodes();
	}

	private void startHub() throws Exception {

		hubAddress = getProperty(SELENIUM_HUB_HOST_PROPERTY,
				SELENIUM_HUB_HOST_DEFAULT);

		hubPort = getProperty(SELENIUM_HUB_PORT_PROPERTY,
				SELENIUM_HUB_PORT_DEFAULT);

		seleniumGridHub = new SeleniumGridHub(hubAddress, hubPort);
		seleniumGridHub.start();
	}

	private void startNodes() throws InterruptedException {
		countDownLatch = new CountDownLatch(nodes.size());

		for (final Node n : nodes) {
			Thread t = new Thread() {
				public void run() {
					try {
						startNode(n);
					} catch (IOException e) {
						log.error("Exception starting node {} : {}",
								n.getAddress(), e.getClass());
					}
				}
			};
			t.start();
		}

		if (!countDownLatch.await(TIMEOUT_NODE, TimeUnit.SECONDS)) {
			Assert.fail("Timeout waiting nodes (" + TIMEOUT_NODE + " seconds)");
		}
	}

	private void startNode(Node node) throws IOException {
		log.info("Launching node {}", node.getAddress());

		final String chromeDriverName = "/chromedriver";
		final String chromeDriverSource = getPathTestFiles()
				+ "/bin/chromedriver/2.9/linux64" + chromeDriverName;
		final String seleniumJarName = "/selenium-server-standalone-2.42.2.jar";
		final String seleniumJarSource = getPathTestFiles()
				+ "/bin/selenium-server" + seleniumJarName;

		// OverThere SCP need absolute path, so home path must be known
		String remoteHome = node.getHome();

		final String remoteFolder = remoteHome + "/" + node.REMOTE_FOLDER;
		final String remoteChromeDriver = remoteFolder + chromeDriverName;
		final String remoteSeleniumJar = remoteFolder + seleniumJarName;
		final String remoteScript = node.getTmpFolder() + "/" + LAUNCH_SH;
		final String remotePort = String.valueOf(node.getRemoteHost()
				.getFreePort());

		if (!node.getRemoteHost().exists(remoteFolder) || node.isOverwrite()) {
			node.getRemoteHost()
					.execAndWaitCommand("mkdir", "-p", remoteFolder);
		}
		if (!node.getRemoteHost().exists(remoteChromeDriver)
				|| node.isOverwrite()) {
			node.getRemoteHost().scp(chromeDriverSource, remoteChromeDriver);
			node.getRemoteHost().execAndWaitCommand("chmod", "+x",
					remoteChromeDriver);
		}
		if (!node.getRemoteHost().exists(remoteSeleniumJar)
				|| node.isOverwrite()) {
			node.getRemoteHost().scp(seleniumJarSource, remoteSeleniumJar);
		}

		// Script is always overwritten
		createRemoteScript(node, remotePort, remoteScript, remoteFolder,
				remoteChromeDriver, remoteSeleniumJar, node.getBrowser(),
				node.getMaxInstances());

		// Copy video in remote host if necessary
		if (node.getVideo() != null
				&& (!node.getRemoteHost().exists(node.getRemoteVideo()) || node
						.isOverwrite())) {
			node.getRemoteHost().scp(node.getVideo(), node.getRemoteVideo());
		}

		// Launch node
		node.getRemoteHost().execCommand(remoteScript);

		// Wait to be available for Hub
		waitForNode(node.getAddress(), remotePort);
	}

	private void createRemoteScript(Node node, String remotePort,
			String remoteScript, String remoteFolder,
			String remoteChromeDriver, String remoteSeleniumJar,
			Browser browser, int maxInstances) throws IOException {

		// Create script for Node
		Configuration cfg = new Configuration();

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("remotePort", String.valueOf(remotePort));
		data.put("maxInstances", String.valueOf(maxInstances));
		data.put("hubIp", hubAddress);
		data.put("hubPort", String.valueOf(hubPort));
		data.put("tmpFolder", node.getTmpFolder());
		data.put("remoteChromeDriver", remoteChromeDriver);
		data.put("remoteSeleniumJar", remoteSeleniumJar);
		data.put("pidFile", node.REMOTE_PID_FILE);
		data.put("browser", browser);

		cfg.setClassForTemplateLoading(PerformanceTest.class, "/templates/");

		String tmpScript = node.getTmpFolder() + LAUNCH_SH;
		try {
			Template template = cfg.getTemplate(LAUNCH_SH + ".ftl");
			Writer writer = new FileWriter(new File(tmpScript));
			template.process(data, writer);
			writer.flush();
			writer.close();

		} catch (Exception e) {
			throw new RuntimeException(
					"Exception while creating file from template", e);
		}

		// Copy script to remote node
		node.getRemoteHost().scp(tmpScript, remoteScript);
		node.getRemoteHost().execAndWaitCommand("chmod", "+x", remoteScript);
		Shell.run("rm", tmpScript);
	}

	private void waitForNode(String node, String port) {
		log.info("Waiting for node {} to be ready...", node);
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
					log.error("Timeout ({} sec) waiting for node {}",
							TIMEOUT_NODE, node);
				}
			}
		} while (responseStatusCode != HttpStatus.SC_OK);

		if (responseStatusCode == HttpStatus.SC_OK) {
			log.info("Node {} ready (responseStatus {})", node,
					responseStatusCode);
			countDownLatch.countDown();
		}
	}

	protected static List<Node> getRandomNodes(int numNodes, Browser browser,
			int maxInstances) {
		return getRandomNodes(numNodes, browser, null, null, maxInstances);
	}

	protected static List<Node> getRandomNodes(int numNodes, Browser browser,
			String video, String audio, int maxInstances) {
		List<Node> nodes = new ArrayList<Node>();

		InputStream inputStream = PerformanceTest.class.getClassLoader()
				.getResourceAsStream("node-list.txt");
		List<String> nodeList = null;
		try {
			nodeList = CharStreams.readLines(new InputStreamReader(inputStream,
					Charsets.UTF_8));
		} catch (IOException e) {
			Assert.fail("Exception reading node-list.txt: " + e.getMessage());
		}

		String nodeCandidate;
		long maxSystemTime = System.currentTimeMillis() + 2 * TIMEOUT_NODE
				* 1000;

		do {
			nodeCandidate = nodeList.get(Randomizer.getInt(0, nodeList.size()));
			log.debug("Node candidate {}", nodeCandidate);

			if (RemoteHost.ping(nodeCandidate)) {
				RemoteHost remoteHost = new RemoteHost(nodeCandidate,
						getProperty("test.node.login"),
						getProperty("test.node.passwd"));
				try {
					remoteHost.start();
					int xvfb = remoteHost.runAndWaitCommand("xvfb-run");
					if (xvfb != 2) {
						log.debug("Node {} has no Xvfb", nodeCandidate);
					} else {
						Node node = new Node(nodeCandidate, browser, video,
								audio);
						node.setMaxInstances(maxInstances);
						nodes.add(node);
					}
				} catch (Exception e) {
					log.debug("Invalid credentials to access node {} ",
							nodeCandidate);
				} finally {
					remoteHost.stop();
				}

			} else {
				log.debug("Node {} seems to be down", nodeCandidate);
			}
			nodeList.remove(nodeCandidate);

			if (System.currentTimeMillis() > maxSystemTime) {
				Assert.fail("Timeout (" + 2 * TIMEOUT_NODE + " sec) selecting "
						+ numNodes + " nodes");
			}

		} while (nodes.size() < numNodes);

		return nodes;
	}

	@After
	public void stopGrid() throws Exception {
		// Stop Hub
		seleniumGridHub.stop();

		// Stop Nodes
		for (Node n : nodes) {
			n.getRemoteHost().execCommand("kill", "-9", "-1");
			n.stopRemoteHost();
		}
	}

	public void runParallel(List<Node> nodeList, Runnable myFunc)
			throws InterruptedException, ExecutionException {
		ExecutorService exec = Executors.newFixedThreadPool(nodes.size());
		List<Future<?>> results = new ArrayList<>();
		for (int i = 0; i < nodes.size(); i++) {
			results.add(exec.submit(myFunc));
		}
		for (Future<?> r : results) {
			r.get();
		}
	}
}
