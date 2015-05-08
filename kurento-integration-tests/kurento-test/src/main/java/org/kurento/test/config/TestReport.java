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
package org.kurento.test.config;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import org.kurento.test.client.BrowserClient;

/**
 * Test report, to compile information of the test result in a HTML page.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.0.0
 */
public class TestReport {

	protected final static String TEST_REPORT_PROPERTY = "test.report";
	protected final static String TEST_REPORT_DEFAULT = "target/report.html";
	protected String testReport = getProperty(TEST_REPORT_PROPERTY,
			TEST_REPORT_DEFAULT);

	protected PrintWriter writer;

	public TestReport() {
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(
					testReport, true)));
			appendTitle("Clearslide tests report [" + new Date() + "]");
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void appendTitle(String text) {
		writer.println("<h1>" + escapeHtml(text) + "</h1>");
		writer.flush();
	}

	public void appendHeader(String text) {
		writer.println("<h2>" + escapeHtml(text) + "</h2>");
		writer.flush();
	}

	public void carriageReturn() {
		writer.println("<br>");
		writer.flush();
	}

	public void appendLine() {
		writer.println("<hr>");
		writer.flush();
	}

	public void appendCode(Object[] text) {
		writer.println("<pre>");
		for (Object o : text) {
			writer.println(escapeHtml(o.toString()));
		}
		writer.println("</pre>");
		writer.flush();
	}

	public void appendText(Object[] text) {
		for (Object o : text) {
			writer.println(escapeHtml(o.toString()) + "<br>");
		}
		writer.flush();
	}

	public void appendText(String text) {
		writer.println(escapeHtml(text));
		writer.flush();
	}

	public void appendWarning(String text) {
		writer.println("<p style='color:orange;font-weight:bold;'>"
				+ escapeHtml(text) + "</p>");
		writer.flush();
	}

	public void appendError(String text) {
		writer.println("<p style='color:red;font-weight:bold;'>"
				+ escapeHtml(text) + "</p>");
		writer.flush();
	}

	public void appendHtml(String html) {
		writer.println(html);
		writer.flush();
	}

	public String escapeHtml(String text) {
		StringBuilder builder = new StringBuilder();
		boolean previousWasASpace = false;
		for (char c : text.toCharArray()) {
			if (c == ' ') {
				if (previousWasASpace) {
					builder.append("&nbsp;");
					previousWasASpace = false;
					continue;
				}
				previousWasASpace = true;
			} else {
				previousWasASpace = false;
			}
			switch (c) {
			case '<':
				builder.append("&lt;");
				break;
			case '>':
				builder.append("&gt;");
				break;
			case '&':
				builder.append("&amp;");
				break;
			case '"':
				builder.append("&quot;");
				break;
			case '\n':
				builder.append("<br>");
				break;
			case '\t':
				builder.append("&nbsp; &nbsp; &nbsp;");
				break;
			default:
				if (c < 128) {
					builder.append(c);
				} else {
					builder.append("&#").append((int) c).append(";");
				}
			}
		}
		return builder.toString();
	}

	public void appendException(Throwable throwable, TestScenario testScenario) {
		appendHtml(throwable.getMessage());
		carriageReturn();
		appendCode(throwable.getStackTrace());
		if (testScenario != null) {
			for (BrowserClient bc : testScenario.getBrowserMap().values()) {
				appendText("Saucelabs jobs");
				carriageReturn();
				if (bc.getScope() == BrowserScope.SAUCELABS) {
					String jobId = bc.getJobId();
					appendHtml("<a href='https://saucelabs.com/tests/" + jobId
							+ "'>https://saucelabs.com/tests/" + jobId
							+ "</a><br>");
				}
			}
		}
	}
}
