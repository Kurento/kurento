package com.kurento.kmf.media;

import org.junit.After;
import org.junit.Before;

public class MediaApiTest {

	protected MediaPipelineFactory pipelineFactory;

	@Before
	public void setupMediaPipelineFactory() {

		String serverAddress = getSystemProperty("kurento.serverAddress",
				"127.0.0.1");
		int serverPort = getSystemProperty("kurento.serverPort", 9090);

		String handlerAddress = getSystemProperty("kurento.handlerAddress",
				"127.0.0.1");
		int handlerPort = getSystemProperty("kurento.handlerPort", 9191);

		pipelineFactory = new MediaPipelineFactory(serverAddress, serverPort,
				handlerAddress, handlerPort);
	}

	@After
	public void teardownMediaPipeline() {
		pipelineFactory.destroy();
	}

	private int getSystemProperty(String property, int defaultValue) {
		String systemValue = System.getProperty(property);
		return systemValue != null ? Integer.parseInt(systemValue)
				: defaultValue;
	}

	private String getSystemProperty(String property, String defaultValue) {
		String systemValue = System.getProperty(property);
		return systemValue != null ? systemValue : defaultValue;
	}

}
