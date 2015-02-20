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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;
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
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserRunner;
import org.kurento.test.client.Client;
import org.kurento.test.monitor.SystemMonitorManager;
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

	public static final String SELENIUM_HUB_PUBLIC_PROPERTY = "selenium.hub.public";
	public static final String SELENIUM_HUB_HOST_PROPERTY = "selenium.hub.host";

	public static final String SELENIUM_NODES_LIST_PROPERTY = "test.nodes.list";
	public static final String SELENIUM_NODES_FILE_LIST_PROPERTY = "test.nodes.file.list";

	public static final String SELENIUM_HUB_HOST_DEFAULT = "127.0.0.1";

	private static final int TIMEOUT_NODE = 300; // seconds
	private static final String LAUNCH_SH = "launch-node.sh";

	private SeleniumGridHub seleniumGridHub;
	private String hubAddress;
	private String hubPublicAddress;
	private int hubPort;
	private CountDownLatch countDownLatch;

	private List<Node> nodes;
	private Node masterNode;

	public PerformanceTest() {

		// Monitor -----------------------
		monitorRate = Integer
				.parseInt(System.getProperty("test.webrtcgrid.monitor",
						String.valueOf(DEFAULT_MONITOR_RATE)));

	}

	public void setNodes(List<Node> nodes) {
		this.nodes = nodes;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	@Before
	public void startGrid() throws Exception {
		startHub();
		startNodes();

		if (masterNode != null) {
			List<Node> list = new ArrayList<>();
			list.add(masterNode);
			startNodes(list);
		}

		// Monitor
		monitor = new SystemMonitorManager();
		monitor.setSamplingTime(monitorRate);
		monitor.start();
	}

	private void startHub() throws Exception {
		hubAddress = getProperty(SELENIUM_HUB_HOST_PROPERTY,
				SELENIUM_HUB_HOST_DEFAULT);
		hubPublicAddress = getProperty(SELENIUM_HUB_PUBLIC_PROPERTY, hubAddress);
		hubPort = getProperty(SELENIUM_HUB_PORT_PROPERTY,
				SELENIUM_HUB_PORT_DEFAULT);

		seleniumGridHub = new SeleniumGridHub(hubAddress, hubPort);
		seleniumGridHub.start();
	}

	private void startNodes() throws InterruptedException {
		startNodes(nodes);
	}

	private void startNodes(List<Node> nodes) throws InterruptedException {
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
		Configuration cfg = new Configuration(
				Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("remotePort", String.valueOf(remotePort));
		data.put("maxInstances", String.valueOf(maxInstances));
		data.put("hubIp", hubPublicAddress);
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

	protected List<Node> getRandomNodes(int numNodes, Browser browser,
			int maxInstances) {
		return getRandomNodes(numNodes, browser, null, null, maxInstances);
	}

	public List<Node> getRandomNodesHttps(int numNodes, Browser browser,
			int maxInstances) {
		return getRandomNodes(numNodes, browser, null, null, maxInstances,
				true, true);
	}

	public List<Node> getRandomNodes(int numNodes, Browser browser,
			String video, String audio, int maxInstances) {
		return getRandomNodes(numNodes, browser, video, audio, maxInstances,
				false, false);
	}

	public List<Node> getRandomNodes(int numNodes, Browser browser,
			String video, String audio, int maxInstances, boolean https,
			boolean screenCapture) {

		List<Node> nodes = new ArrayList<Node>();

		String nodesListProp = System.getProperty(SELENIUM_NODES_LIST_PROPERTY);
		String nodesListFileProp = System
				.getProperty(SELENIUM_NODES_FILE_LIST_PROPERTY);

		List<String> nodeList = null;
		if (nodesListFileProp != null) {
			try {
				nodeList = FileUtils.readLines(new File(nodesListFileProp),
						Charset.defaultCharset());
			} catch (IOException e) {
				Assert.fail("Exception reading node list file: "
						+ e.getMessage());
			}
		} else if (nodesListProp != null) {
			nodeList = new ArrayList<>(Arrays.asList(nodesListProp.split(";")));
		} else {
			InputStream inputStream = PerformanceTest.class.getClassLoader()
					.getResourceAsStream("node-list.txt");

			try {
				nodeList = CharStreams.readLines(new InputStreamReader(
						inputStream, Charsets.UTF_8));
			} catch (IOException e) {
				Assert.fail("Exception reading node-list.txt: "
						+ e.getMessage());
			}
		}

		String nodeCandidate;
		long maxSystemTime = System.currentTimeMillis() + 2 * TIMEOUT_NODE
				* 1000;

		do {
			try {
				nodeCandidate = nodeList.get(Randomizer.getInt(0,
						nodeList.size()));
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(
						"No valid available node(s) to perform Selenim Grid test");
			}
			log.debug("Node candidate {}", nodeCandidate);

			if (!nodeCandidate.isEmpty()) {
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
							if (https) {
								node.setHttps();
							}
							if (screenCapture) {
								node.setEnableScreenCapture();
							}
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
			}
			nodeList.remove(nodeCandidate);

			if (System.currentTimeMillis() > maxSystemTime) {
				Assert.fail("Timeout (" + 2 * TIMEOUT_NODE + " sec) selecting "
						+ numNodes + " nodes");
			}

		} while (nodes.size() < numNodes);

		String nodeListStr = "";
		for (Node node : nodes) {
			nodeListStr += node.getAddress() + " ";
		}
		log.debug("Node list: {}", nodeListStr);

		return nodes;
	}

	@After
	public void stopGrid() throws Exception {
		// Stop Hub
		seleniumGridHub.stop();

		// Stop Nodes
		for (Node node : nodes) {
			stopNode(node);
		}

		// Stop master node (if any)
		if (masterNode != null) {
			stopNode(masterNode);
		}

		// Monitor
		monitor.stop();
		monitor.writeResults(getDefaultOutputFile("-monitor.csv"));
		monitor.destroy();
	}

	private void stopNode(Node node) throws IOException {
		node.getRemoteHost().execCommand("kill", "-9", "-1");
		node.stopRemoteHost();
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

	// ------------------ Monitor -----------------

	private static final int DEFAULT_MONITOR_RATE = 1000; // milliseconds
	private static final int DEFAULT_NBROWSERS = 2; // Browser per node
	private static final int DEFAULT_CLIENT_RATE = 5000; // milliseconds
	private static final int DEFAULT_TIMEOUT = 60; // milliseconds

	public SystemMonitorManager monitor;

	private int monitorRate = DEFAULT_MONITOR_RATE;
	private int browserCreationTime = DEFAULT_CLIENT_RATE;
	private int numBrowsersPerNode = DEFAULT_NBROWSERS;
	private int timeout = DEFAULT_TIMEOUT;

	protected void incrementNumClients() throws IOException {
		monitor.incrementNumClients();
	}

	protected void decrementNumClients() throws IOException {
		monitor.decrementNumClients();
	}

	public void setMasterNode(Node masterNode) {
		this.masterNode = masterNode;
	}

	// ----------------------------------------------

	public int getAllBrowsersStartedTime() {
		return nodes.size() * numBrowsersPerNode * browserCreationTime;
	}

	public void setBrowserCreationRate(int browserCreationTime) {
		this.browserCreationTime = browserCreationTime;
	}

	public void setNumBrowsersPerNode(int numBrowserPerNode) {
		this.numBrowsersPerNode = numBrowserPerNode;
	}

	public void parallelBrowsers(BrowserRunner browserRunner, Client client) {
		parallelBrowsers(browserRunner, client, 0);
	}

	public void parallelBrowsers(final BrowserRunner browserRunner,
			final Client client, final int port) {
		final ExecutorService internalExec = Executors.newFixedThreadPool(nodes
				.size() * numBrowsersPerNode);

		CompletionService<Void> exec = new ExecutorCompletionService<>(
				internalExec);

		int numBrowser = 0;
		for (final Node node : getNodes()) {
			for (int i = 1; i <= numBrowsersPerNode; i++) {

				final String name = node.getAddress() + "-browser" + i
						+ "-count" + (numBrowser + 1);

				final int numBrowserFinal = numBrowser;

				exec.submit(new Callable<Void>() {
					public Void call() throws Exception {
						try {
							Thread.currentThread().setName(name);
							Thread.sleep(browserCreationTime * numBrowserFinal);
							log.debug("*** Starting node {} ***", name);
							incrementNumClients();

							BrowserClient browser = null;

							// Browser
							BrowserClient.Builder builder;
							if (port != 0) {
								builder = new BrowserClient.Builder(port);
							} else {
								builder = new BrowserClient.Builder();
							}
							builder = builder.browser(node.getBrowser())
									.client(client).remoteNode(node);

							if (node.getVideo() != null) {
								builder = builder.video(node.getVideo());
							}
							if (node.isHttps()) {
								builder = builder.useHttps();
							}
							if (node.isEnableScreenCapture()) {
								builder = builder.enableScreenCapture();
							}

							browser = builder.build();

							browser.setMonitor(monitor);
							monitor.addRtcStats(browser);

							browserRunner.run(browser, numBrowserFinal, name);
						} finally {
							decrementNumClients();
							log.debug("--- Ending client {} ---", name);
						}
						return null;
					}
				});
				numBrowser++;
			}
		}

		for (int i = 1; i <= getNodes().size() * numBrowsersPerNode; i++) {
			Future<Void> taskFuture = null;
			try {
				taskFuture = exec.take();
				taskFuture.get(timeout, TimeUnit.SECONDS);
			} catch (Throwable e) {
				log.error("$$$ {} $$$", e.getCause().getMessage());
				e.printStackTrace();
				if (taskFuture != null) {
					taskFuture.cancel(true);
				}
			} finally {
				log.debug("+++ Ending browser #{} +++", i);
			}
		}
	}

	public void parallelBrowsers(final BrowserRunner browserRunner) {
		parallelBrowsers(browserRunner, Client.WEBRTC);
	}

}
