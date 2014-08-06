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
package org.kurento.kmf.test.client;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.jmeter.JMeter;
import org.w3c.dom.Document;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Class that models a client used to performance tests; it uses JMeter as
 * testing framework.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.2.3
 * @see <a href="http://jmeter.apache.org/">Apache JMeter</a>
 */
public class JMeterClient {

	final static String ROOT = "src/test/resources/jmeter/";
	final static String JMXFOLDER = "target/test-classes/";
	final static String REPORTS = "target/jmeter-reports/";
	final static String JMXFILE = "player.jmx";

	private EventListener eventListener;
	private URL url;
	private int concurrentUsers;
	private CountDownLatch terminationEvent;
	private int timeout; // seconds

	public JMeterClient(URL url) {
		this.url = url;
		terminationEvent = new CountDownLatch(1);

		// Default number of concurrent users = 5
		this.concurrentUsers = 5;

		// default timeout = 60 seconds
		timeout = 60;
	}

	private void setup() throws TemplateException, IOException {
		Configuration cfg = new Configuration();
		// Load template from source folder
		Template template = cfg.getTemplate(ROOT + JMXFILE + ".ftl");

		// Build the data-model
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("protocol", url.getProtocol());
		data.put("port", String.valueOf(url.getPort()));
		data.put("host", url.getHost());
		data.put("path", url.getPath());
		data.put("numUsers", concurrentUsers);

		// File output
		Writer file = new FileWriter(new File(JMXFOLDER + JMXFILE));
		template.process(data, file);
		file.flush();
		file.close();
	}

	public void start() throws Exception {
		// Subscribe to termination event
		this.addEventListener(new EventListener() {
			@Override
			public void onEvent(String event) {
				terminationEvent.countDown();
			}
		});

		// Setup
		setup();
		final String jmxExtension = ".jmx";
		final String jtlExtension = ".jtl";
		final String htmlExtension = ".html";
		final String logFile = REPORTS + "jmeter.log";
		final String jmeterProperties = ROOT + "bin/jmeter.properties";
		final File xsl = new File(ROOT + "bin/jmeter-results-detail.xsl");

		File fileFeports = new File(REPORTS);
		if (!fileFeports.exists()) {
			fileFeports.mkdir();
		}
		JMeter jmeter = new JMeter();
		String[] files = new File(JMXFOLDER).list();

		for (String file : files) {
			if (file.toLowerCase().endsWith(jmxExtension)) {
				// Launching JMeter for each JMX test plan found
				String fileNoExt = file.substring(0, file.length()
						- jmxExtension.length());
				String outputFileJtl = REPORTS + fileNoExt + jtlExtension;
				String[] arguments = { "-n", "-t", JMXFOLDER + file, "-p",
						jmeterProperties, "-d", ROOT, "-l", outputFileJtl,
						"-j", logFile };
				jmeter.start(arguments);

				// Waiting for JMeter ending
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				System.setOut(new PrintStream(baos));
				boolean running = true;
				do {
					BufferedReader br = new BufferedReader(new StringReader(
							baos.toString()));
					String line;
					while ((line = br.readLine()) != null) {
						running = !line.equals("... end of run");
					}
				} while (running);

				// Transform raw JTL to friendly HTML using XSL
				String outputFileHtml = REPORTS + fileNoExt + htmlExtension;
				jtl2html(xsl, new File(outputFileJtl), new File(outputFileHtml));
			}
		}

		// End
		File jmxFile = new File(JMXFOLDER + JMXFILE);
		jmxFile.delete();
	}

	private void jtl2html(File stylesheet, File datafile, File fileOutput)
			throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(datafile);

		TransformerFactory tFactory = TransformerFactory.newInstance();
		StreamSource stylesource = new StreamSource(stylesheet);
		Transformer transformer = tFactory.newTransformer(stylesource);

		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(fileOutput);
		transformer.transform(source, result);

		// callback
		eventListener.onEvent("end");
	}

	public boolean waitForEnding() throws InterruptedException {
		return terminationEvent.await(getTimeout(), TimeUnit.SECONDS);
	}

	public void addEventListener(final EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	public int getConcurrentUsers() {
		return concurrentUsers;
	}

	public void setConcurrentUsers(int concurrentUsers) {
		this.concurrentUsers = concurrentUsers;
	}

	public URL getUrl() {
		return url;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

}
