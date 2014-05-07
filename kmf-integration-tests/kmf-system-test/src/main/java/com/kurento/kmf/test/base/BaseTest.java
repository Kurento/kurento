package com.kurento.kmf.test.base;

import java.io.IOException;

import org.junit.Rule;
import org.junit.rules.TestName;

import com.kurento.kmf.test.KurentoMediaServer;
import com.kurento.kmf.test.PortManager;
import com.kurento.kmf.test.PropertiesManager;

import freemarker.template.TemplateException;

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

}
