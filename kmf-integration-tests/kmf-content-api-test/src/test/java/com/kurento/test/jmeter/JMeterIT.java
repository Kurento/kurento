/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
package com.kurento.test.jmeter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.jmeter.JMeter;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

import com.kurento.test.base.BaseArquillianTst;

/**
 * Performance test for Kurento Media Framework; this test uses JUnit/Arquillian
 * base, but also <b>JMeter</b> to carry out performance testing using several
 * concurrent requests to the Kurento Application Server (KAS). This test reads
 * each JMeter test plan (<code>.jmx</code> files) located under
 * <code>src/test/resources</code> folder and execute these test plans. HTML
 * reports are generated with the result of theses tests on
 * <code>target/jmeter-reports</code> folder.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 * @see <a href="http://jmeter.apache.org/">Apache JMeter</a>
 */
@RunWith(Arquillian.class)
public class JMeterIT extends BaseArquillianTst {

	@Test
	public void testPerformance() throws Exception {
		final String root = "src/test/jmeter/";
		final String jmxFolder = "target/test-classes/";
		final String reports = "target/jmeter-reports/";
		final String jmxExtension = ".jmx";
		final String jtlExtension = ".jtl";
		final String htmlExtension = ".html";
		final String logFile = reports + "jmeter.log";
		final String jmeterProperties = root + "bin/jmeter.properties";
		final File xsl = new File(root + "bin/jmeter-results-detail.xsl");

		File fileFeports = new File(reports);
		if (!fileFeports.exists()) {
			fileFeports.mkdir();
		}
		JMeter jmeter = new JMeter();
		String[] files = new File(jmxFolder).list();

		for (String file : files) {
			if (file.toLowerCase().endsWith(jmxExtension)) {
				// Launching JMeter for each JMX test plan found
				String fileNoExt = file.substring(0, file.length()
						- jmxExtension.length());
				String outputFileJtl = reports + fileNoExt + jtlExtension;
				String[] arguments = { "-n", "-t", jmxFolder + file, "-p",
						jmeterProperties, "-d", root, "-l", outputFileJtl,
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
				String outputFileHtml = reports + fileNoExt + htmlExtension;
				jtl2html(xsl, new File(outputFileJtl), new File(outputFileHtml));
			}
		}
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
	}
}
