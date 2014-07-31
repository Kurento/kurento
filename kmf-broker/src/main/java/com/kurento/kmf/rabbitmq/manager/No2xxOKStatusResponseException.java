package com.kurento.kmf.rabbitmq.manager;

import org.apache.http.client.methods.CloseableHttpResponse;

public class No2xxOKStatusResponseException extends RuntimeException {

	private static final long serialVersionUID = -8544067667608547298L;

	private CloseableHttpResponse response;

	public No2xxOKStatusResponseException(CloseableHttpResponse response) {
		super("Status " + response.getStatusLine().getStatusCode());
		this.response = response;
	}

	public CloseableHttpResponse getResponse() {
		return response;
	}

	@Override
	public String toString() {
		return response.toString();
	}

}
