package com.kurento.kmf.rabbitmq;

public class RabbitMqException extends RuntimeException {

	private static final long serialVersionUID = 8339661146128257545L;

	public RabbitMqException(String message) {
		super(message);
	}

	public RabbitMqException(String message, Throwable cause) {
		super(message, cause);
	}
}
