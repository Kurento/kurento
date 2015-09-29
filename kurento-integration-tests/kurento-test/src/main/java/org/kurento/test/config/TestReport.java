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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import org.kurento.test.browser.Browser;

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
	protected final static String RETURN = "\r\n";
	protected final static int WIDTH_PERCENTAGE = 95;

	protected PrintWriter writer;
	protected String extraInfoHtml;
	protected String extraErrorHtml;
	protected int numRetries;

	public TestReport(String name, int numRetries) {
		this(name, numRetries, null);
	}
	
	public TestReport(String name, int numRetries, String htmlHeader) {
		try {
			this.numRetries = numRetries;
			this.extraInfoHtml = "";
			this.extraErrorHtml = "";
			String title = (name == null) ? "Tests report [" + new Date() + "]"
					: name;
			File file = new File(testReport);
			boolean exists = file.exists();
			writer = new PrintWriter(new BufferedWriter(new FileWriter(file,
					true)));
			if (!exists) {
				initPage();
				if (htmlHeader != null) {
					appendHtml(htmlHeader);
				}
			}
			appendTitle(title);
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void initPage() {
		appendHtml("<script src='//cdn.rawgit.com/eligrey/FileSaver.js/master/FileSaver.js'></script>");
		appendHtml("<script>");
		appendHtml("window.onload = function() {");
		appendHtml("var allTextPage = document.documentElement.innerText;");
		appendHtml("var ok = (allTextPage.match(/Test ok/g) || []).length;");
		appendHtml("var retries = (allTextPage.match(/Test failed in retry/g) || []).length;");
		appendHtml("var errors = (allTextPage.match(/TEST ERROR/g) || []).length;");
		appendHtml("var summary = document.getElementById('summary');");
		appendHtml("var executions = ok + retries;");
		appendHtml("var tests = ok + errors;");
		appendHtml("var retriesOk = retries - errors*" + numRetries + ";");
		appendHtml("summary.innerHTML += \"<p style='color:black;font-weight:bold;'>Number of test(s): \" + tests + \"</p>\";");
		appendHtml("if (tests != executions) summary.innerHTML += \"Number of test(s) executions: \" + executions + \"</p>\";");
		appendHtml("if (ok > 0) summary.innerHTML += \"<p style='color:green;font-weight:bold;'>Number of test(s) ok: \" + ok + \"</p>\";");
		appendHtml("if (retries > 0) summary.innerHTML += \"<p style='color:orange;font-weight:bold;'>Number of test(s) with retry: \" + retries + \"</p>\";");
		appendHtml("if (retriesOk > 0) summary.innerHTML += \"<p style='color:orange;font-weight:bold;'>Number of test(s) retried and succeeded: \" + retriesOk + \"</p>\";");
		appendHtml("if (errors > 0) summary.innerHTML += \"<p style='color:red;font-weight:bold;'>Number of test(s) with error (after "
				+ numRetries + " retries): \" + errors + \"</p>\";");
		appendHtml("}");
		appendHtml("</script>");
		appendHtml("<div style='width:"
				+ WIDTH_PERCENTAGE
				+ "%; border: 1px solid grey;' id='summary'><h1>Test report summary</h1><hr></div>");
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
		writer.println("<hr style='margin-left:0; width:" + WIDTH_PERCENTAGE
				+ "%;'>");
		writer.flush();
	}

	public String getCode(String text) {
		String code = "<button type='button' onclick=\"saveAs(new Blob([nextSibling.value], "
				+ "{type: 'text/plain;charset=utf-8'}), previousSibling.innerText ? "
				+ "previousSibling.innerText : previousSibling.previousSibling.innerText "
				+ "+ '.log');\">Save</button>";
		code += "<textarea readonly style='width:" + WIDTH_PERCENTAGE
				+ "%; height:150px;' wrap='off'>";
		code += text;
		code += "</textarea><br><br>";
		return code;
	}

	public void appendCode(String text) {
		writer.println(getCode(text));
		writer.flush();
	}

	public void appendCode(Object[] text) {
		String allText = "";
		for (Object o : text) {
			allText += o.toString() + RETURN;
		}
		appendCode(allText);
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

	public void appendSuccess(String text) {
		writer.println("<p style='color:green;font-weight:bold;'>"
				+ escapeHtml(text) + "</p>");
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

	public void appendTrace(String text) {
		writer.println("<pre style='width:" + WIDTH_PERCENTAGE + "%'>" + text
				+ "</pre>");
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
		appendHtml("<b>Error description</b>");
		appendCode(throwable.getClass().getName() + " : "
				+ throwable.getMessage());
		appendHtml("<b>Error trace</b>");
		appendCode(throwable.getStackTrace());
		boolean saucelabsTitle = false;
		if (testScenario != null) {
			for (Browser bc : testScenario.getBrowserMap().values()) {
				if (bc.getScope() == BrowserScope.SAUCELABS) {
					if (!saucelabsTitle) {
						appendHtml("<b>Saucelabs job(s)</b><br>");
						saucelabsTitle = true;
					}
					String jobId = bc.getJobId();
					appendHtml("<a href='https://saucelabs.com/tests/" + jobId
							+ "'>" + bc.getId() + "</a><br>");
				}
			}
			if (saucelabsTitle) {
				carriageReturn();
			}
		}
	}

	public void addExtraInfoText(String text) {
		extraInfoHtml += escapeHtml(text);
	}

	public void addExtraInfoHtml(String html) {
		extraInfoHtml += html;
	}

	public void addExtraErrorText(String text) {
		extraErrorHtml += escapeHtml(text);
	}

	public void addExtraErrorHtml(String html) {
		extraErrorHtml += html;
	}

	public void flushExtraInfoHtml() {
		if (!extraInfoHtml.isEmpty()) {
			appendHtml(extraInfoHtml);
			extraInfoHtml = "";
		}
	}

	public void flushExtraErrorHtml() {
		if (!extraErrorHtml.isEmpty()) {
			appendHtml(extraErrorHtml);
			extraErrorHtml = "";
		}
	}
}
