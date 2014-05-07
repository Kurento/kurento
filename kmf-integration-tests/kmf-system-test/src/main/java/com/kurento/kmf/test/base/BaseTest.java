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

import java.io.IOException;

import org.junit.Rule;
import org.junit.rules.TestName;

import com.kurento.kmf.test.KurentoMediaServer;
import com.kurento.kmf.test.PortManager;
import com.kurento.kmf.test.PropertiesManager;

import freemarker.template.TemplateException;

/**
 * Base for tests (Content and Media API).
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.2.3
 */
public class BaseTest {

	@Rule
	public TestName testName = new TestName();

	protected boolean autostart;
	protected KurentoMediaServer kms;

	protected String serverAddress;
	protected int serverPort;
	protected String handlerAddress;
	protected int handlerPort;

	protected int httpEndpointPort;

	protected void setupKurentoServer() throws IOException, TemplateException,
			InterruptedException {

		serverAddress = PropertiesManager.getSystemProperty(
				"kurento.serverAddress", "127.0.0.1");
		serverPort = PropertiesManager.getSystemProperty("kurento.serverPort",
				9090);
		handlerAddress = PropertiesManager.getSystemProperty(
				"kurento.handlerAddress", "127.0.0.1");
		handlerPort = PropertiesManager.getSystemProperty(
				"kurento.handlerPort", 9106);

		httpEndpointPort = PropertiesManager.getSystemProperty(
				"httpEPServer.serverPort", 9091);

		// KMS
		autostart = PropertiesManager.getSystemProperty("kurento.autostart",
				true);
		kms = new KurentoMediaServer(serverAddress, serverPort,
				httpEndpointPort);
		if (autostart && kms.isConfigAvailable()) {

			kms.start(testName.getMethodName());
		}
	}

	protected void teardownKurentoServer() {
		if (autostart && kms.isConfigAvailable()) {
			kms.stop();
		}
	}

	public int getServerPort() {
		return PortManager.getPort();
	}

	public KurentoMediaServer getKms() {
		return kms;
	}

	/**
	 * Compares two numbers and return true|false if these number are similar,
	 * using a threshold in the comparison.
	 * 
	 * @param i
	 *            First number to be compared
	 * @param j
	 *            Second number to be compared
	 * @param threslhold
	 *            Comparison threslhold
	 * @return true|false
	 */
	public boolean compare(double i, double j, int threslhold) {
		return (j - i) <= (i * threslhold / 100);
	}

}
