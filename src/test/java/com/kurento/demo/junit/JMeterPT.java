package com.kurento.demo.junit;

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

@RunWith(Arquillian.class)
public class JMeterPT extends BaseArquillianTst {

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
				String outputFileJtl = reports + file + jtlExtension;
				String[] arguments = { "-n", "-t", jmxFolder + file, "-p",
						jmeterProperties, "-d", root, "-l", outputFileJtl,
						"-j", logFile };
				jmeter.start(arguments);

				// Waiting for JMeter ending
				PrintStream out = System.out;
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
				System.setOut(out);

				// Transform raw JTL to friendly HTML using XSL
				String outputFileHtml = reports + file + htmlExtension;
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
