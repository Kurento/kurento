package com.kurento.kmf.broker.test.mock;

import kmf.broker.server.MediaServerBroker;

public class MediaServer {

	private MediaServerBroker mediaServerBroker;
	private int num;

	public MediaServer(int num) {
		this.num = num;
	}

	public void start() {

		String serverAddress = getSystemProperty("kurento.serverAddress",
				"127.0.0.1");
		int serverPort = getSystemProperty("kurento.serverPort", 9090);

		String handlerAddress = getSystemProperty("kurento.handlerAddress",
				"127.0.0.1");
		int handlerPort = getSystemProperty("kurento.handlerPort", 9191 + num);

		mediaServerBroker = new MediaServerBroker(serverAddress, serverPort,
				handlerAddress, handlerPort);
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
