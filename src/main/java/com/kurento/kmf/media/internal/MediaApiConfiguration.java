package com.kurento.kmf.media.internal;

public class MediaApiConfiguration {
	private String serverAddress;
	private int serverPort;
	private String handlerAddress;
	private int handlerPort;

	private int poolSize = 5;

	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public String getHandlerAddress() {
		return handlerAddress;
	}

	public void setHandlerAddress(String handlerAddress) {
		this.handlerAddress = handlerAddress;
	}

	public int getHandlerPort() {
		return handlerPort;
	}

	public void setHandlerPort(int handlerPort) {
		this.handlerPort = handlerPort;
	}

	public int getPoolSize() {
		return poolSize;
	}

	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}
}
