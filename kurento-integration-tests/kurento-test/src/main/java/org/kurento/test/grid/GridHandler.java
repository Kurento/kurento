/*

 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
package org.kurento.test.grid;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.TestConfiguration.SELENIUM_HUB_ADDRESS;
import static org.kurento.test.TestConfiguration.SELENIUM_HUB_ADDRESS_DEFAULT;
import static org.kurento.test.TestConfiguration.SELENIUM_HUB_PORT_DEFAULT;
import static org.kurento.test.TestConfiguration.SELENIUM_HUB_PORT_PROPERTY;
import static org.kurento.test.TestConfiguration.SELENIUM_NODES_FILE_LIST_PROPERTY;
import static org.kurento.test.TestConfiguration.SELENIUM_NODES_LIST_DEFAULT;
import static org.kurento.test.TestConfiguration.SELENIUM_NODES_LIST_PROPERTY;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
import org.junit.Assert;
import org.kurento.test.Shell;
import org.kurento.test.base.PerformanceTest;
import org.kurento.test.client.BrowserType;
import org.kurento.test.config.TestScenario;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.kurento.test.services.Randomizer;
import org.kurento.test.services.SshConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Singleton handler for Selenium Grid infrastructure.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.1.1
 * @see <a href="http://www.seleniumhq.org/">Selenium</a>
 */
public class GridHandler {

	public Logger log = LoggerFactory.getLogger(GridHandler.class);

	private static GridHandler instance = null;

	private static final int TIMEOUT_NODE = 300; // seconds
	private static final String LAUNCH_SH = "launch-node.sh";

	private GridHub hub;
	private String hubAddress = getProperty(SELENIUM_HUB_ADDRESS, SELENIUM_HUB_ADDRESS_DEFAULT);
	private int hubPort = getProperty(SELENIUM_HUB_PORT_PROPERTY, SELENIUM_HUB_PORT_DEFAULT);
	private CountDownLatch countDownLatch;
	private Map<String, GridNode> nodes = new ConcurrentHashMap<>();
	private List<String> nodeList;
	private boolean hubStarted = false;

	protected GridHandler() {
		String nodesListProp = System.getProperty(SELENIUM_NODES_LIST_PROPERTY);
		String nodesListFileProp = System.getProperty(SELENIUM_NODES_FILE_LIST_PROPERTY);

		if (nodesListFileProp != null) {
			try {
				nodeList = FileUtils.readLines(new File(nodesListFileProp), Charset.defaultCharset());
			} catch (IOException e) {
				Assert.fail("Exception reading node list file: " + e.getMessage());
			}
		} else if (nodesListProp != null) {
			nodeList = new ArrayList<>(Arrays.asList(nodesListProp.split(";")));
		} else {
			InputStream inputStream = PerformanceTest.class.getClassLoader()
					.getResourceAsStream(SELENIUM_NODES_LIST_DEFAULT);

			try {
				nodeList = CharStreams.readLines(new InputStreamReader(inputStream, Charsets.UTF_8));
			} catch (IOException e) {
				Assert.fail("Exception reading node-list.txt: " + e.getMessage());
			}
		}
	}

	public static synchronized GridHandler getInstance() {
		if (instance == null) {
			instance = new GridHandler();
		}
		return instance;
	}

	public synchronized void stopGrid() {
		log.info("Stopping Selenium Grid");
		try {
			// Stop Hub
			if (hub != null) {
				log.info("Stopping Hub");
				hub.stop();
				hubStarted = false;
			}

			// Stop Nodes
			if (nodes != null) {
				log.info("Number of nodes: {}", nodes.size());

				for (GridNode node : nodes.values()) {
					log.info("Stopping Node {}", node.getHost());
					stopNode(node);
				}
			}
			nodes.clear();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public synchronized void startHub() {
		try {
			if (hubAddress != null && !hubStarted) {
				hub = new GridHub(hubAddress, hubPort);
				hub.start();
				hubStarted = true;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void startNodes() {
		try {
			countDownLatch = new CountDownLatch(nodes.size());
			ExecutorService exec = Executors.newFixedThreadPool(nodes.size());

			for (final GridNode n : nodes.values()) {
				Thread t = new Thread() {
					public void run() {
						startNode(n);
					}
				};
				exec.execute(t);
			}

			if (!countDownLatch.await(TIMEOUT_NODE, TimeUnit.SECONDS)) {
				Assert.fail("Timeout waiting nodes (" + TIMEOUT_NODE + " seconds)");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void startNode(GridNode node) {
		try {
			countDownLatch = new CountDownLatch(1);
			log.info("Launching node {}", node.getHost());
			node.startSsh();

			final String chromeDriverName = "/chromedriver";
			final String chromeDriverSource = KurentoServicesTestHelper.getTestFilesPath()
					+ "/bin/chromedriver/2.9/linux64" + chromeDriverName;
			final String seleniumJarName = "/selenium-server-standalone-2.42.2.jar";
			final String seleniumJarSource = KurentoServicesTestHelper.getTestFilesPath() + "/bin/selenium-server"
					+ seleniumJarName;

			// OverThere SCP need absolute path, so home path must be known
			String remoteHome = node.getHome();

			final String remoteFolder = remoteHome + "/" + node.REMOTE_FOLDER;
			final String remoteChromeDriver = remoteFolder + chromeDriverName;
			final String remoteSeleniumJar = remoteFolder + seleniumJarName;
			final String remoteScript = node.getTmpFolder() + "/" + LAUNCH_SH;
			final String remotePort = String.valueOf(node.getSshConnection().getFreePort());

			if (!node.getSshConnection().exists(remoteFolder) || node.isOverwrite()) {
				node.getSshConnection().execAndWaitCommand("mkdir", "-p", remoteFolder);
			}
			if (!node.getSshConnection().exists(remoteChromeDriver) || node.isOverwrite()) {
				node.getSshConnection().scp(chromeDriverSource, remoteChromeDriver);
				node.getSshConnection().execAndWaitCommand("chmod", "+x", remoteChromeDriver);
			}
			if (!node.getSshConnection().exists(remoteSeleniumJar) || node.isOverwrite()) {
				node.getSshConnection().scp(seleniumJarSource, remoteSeleniumJar);
			}

			// Script is always overwritten
			createRemoteScript(node, remotePort, remoteScript, remoteFolder, remoteChromeDriver, remoteSeleniumJar,
					node.getBrowserType(), node.getMaxInstances());

			// Launch node
			node.getSshConnection().execCommand(remoteScript);

			// Wait to be available for Hub
			waitForNode(node.getHost(), remotePort);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void createRemoteScript(GridNode node, String remotePort, String remoteScript, String remoteFolder,
			String remoteChromeDriver, String remoteSeleniumJar, BrowserType browser, int maxInstances)
					throws IOException {

		// Create script for Node
		Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("remotePort", String.valueOf(remotePort));
		data.put("maxInstances", String.valueOf(maxInstances));
		// data.put("hubIp", hubPublicAddress);
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
			throw new RuntimeException("Exception while creating file from template", e);
		}

		// Copy script to remote node
		node.getSshConnection().scp(tmpScript, remoteScript);
		node.getSshConnection().execAndWaitCommand("chmod", "+x", remoteScript);
		Shell.run("rm", tmpScript);
	}

	public void copyRemoteVideo(GridNode node, String video) {
		try {
			// Copy video in remote host if necessary
			if (!node.getSshConnection().exists(node.getRemoteVideo(video)) || node.isOverwrite()) {
				node.getSshConnection().scp(video, node.getRemoteVideo(video));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void waitForNode(String node, String port) {
		log.info("Waiting for node {} to be ready...", node);
		int responseStatusCode = 0;
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet httpGet = new HttpGet("http://" + node + ":" + port + "/wd/hub/static/resource/hub.html");

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
					log.error("Timeout ({} sec) waiting for node {}", TIMEOUT_NODE, node);
				}
			}
		} while (responseStatusCode != HttpStatus.SC_OK);

		if (responseStatusCode == HttpStatus.SC_OK) {
			log.info("Node {} ready (responseStatus {})", node, responseStatusCode);
			countDownLatch.countDown();
		}
	}

	public synchronized GridNode getRandomNodeFromList(String browserKey, BrowserType browserType,
			int browserPerInstance) {

		GridNode node = browserPerInstance > 1 ? existsNode(browserKey) : null;
		String nodeCandidate;
		long maxSystemTime = System.currentTimeMillis() + 2 * TIMEOUT_NODE * 1000;

		if (node == null) {
			boolean validCandidateFound = false;
			do {
				try {
					nodeCandidate = nodeList.get(Randomizer.getInt(0, nodeList.size()));
				} catch (IllegalArgumentException e) {
					throw new RuntimeException("No valid available node(s) to perform Selenim Grid test");
				}
				log.debug("Node candidate {}", nodeCandidate);

				if (!nodeCandidate.isEmpty()) {
					if (SshConnection.ping(nodeCandidate)) {
						SshConnection remoteHost = new SshConnection(nodeCandidate);
						try {
							remoteHost.start();
							int xvfb = remoteHost.runAndWaitCommand("xvfb-run");
							if (xvfb != 2) {
								log.debug("Node {} has no Xvfb", nodeCandidate);
							} else {
								node = new GridNode(nodeCandidate, browserType, browserPerInstance);

								log.info(">>>> Using node {} for browser '{}'", node.getHost(), browserKey);

								nodes.put(browserKey, node);
								validCandidateFound = true;
							}
						} catch (Exception e) {
							log.debug("Invalid credentials to access node {} ", nodeCandidate);
						} finally {
							remoteHost.stop();
						}

					} else {
						log.debug("Node {} seems to be down", nodeCandidate);
					}
				}
				nodeList.remove(nodeCandidate);
			} while (!validCandidateFound);

			if (System.currentTimeMillis() > maxSystemTime) {
				throw new RuntimeException("Timeout (" + 2 * TIMEOUT_NODE + " sec) selecting 1 node");
			}
		} else {
			log.info(">>>> Re-using node {} for browser '{}'", node.getHost(), browserKey);
		}
		return node;

	}

	private GridNode existsNode(String browserKey) {
		GridNode gridNode = null;
		int indexOfSeparator = browserKey.lastIndexOf(TestScenario.INSTANCES_SEPARATOR);

		if (indexOfSeparator != -1) {
			String browserPreffix = browserKey.substring(0, indexOfSeparator);

			for (String node : nodes.keySet()) {
				if (node.startsWith(browserPreffix)) {
					gridNode = nodes.get(node);
					break;
				}
			}
		}
		log.debug("Exists node {} = {}", browserKey, gridNode != null);

		return gridNode;
	}

	private void stopNode(GridNode node) throws IOException {
		if (node.getSshConnection().isStarted()) {
			node.getSshConnection().execCommand("kill", "-9", "-1");
			node.stopSsh();
		}
	}

	public void runParallel(List<GridNode> nodeList, Runnable myFunc) throws InterruptedException, ExecutionException {
		ExecutorService exec = Executors.newFixedThreadPool(nodes.size());
		List<Future<?>> results = new ArrayList<>();
		for (int i = 0; i < nodes.size(); i++) {
			results.add(exec.submit(myFunc));
		}
		for (Future<?> r : results) {
			r.get();
		}
	}

	public String getHubHost() {
		return hubAddress;
	}

	public int getHubPort() {
		return hubPort;
	}

	public GridNode getNode(String browserKey) {
		return nodes.get(browserKey);
	}

	public synchronized void addNode(String browserKey, GridNode node) {
		nodes.put(browserKey, node);
	}

	public boolean useRemoteNodes() {
		return !nodes.isEmpty();
	}

	public void logNodeList() {
		String nodeListStr = "";
		for (GridNode node : nodes.values()) {
			nodeListStr += node.getHost() + " ";
		}
		log.debug("Node list: {}", nodeListStr);
	}

	public GridNode getFirstNode(String browserKey) {
		if (nodes.containsKey(browserKey)) {
			return nodes.get(browserKey);
		} else {
			return nodes.get(browserKey.substring(0, browserKey.indexOf("-") + 1) + 0);
		}
	}

	public boolean containsSimilarBrowserKey(String browserKey) {
		boolean out = false;
		int index = browserKey.indexOf("-");
		if (index != -1) {
			String prefix = browserKey.substring(0, browserKey.indexOf("-"));
			for (String key : nodes.keySet()) {
				out |= key.startsWith(prefix);
				if (out) {
					break;
				}
			}
		}
		return out;
	}

	public void setHubAddress(String hubAddress) {
		this.hubAddress = hubAddress;
	}

}
