package com.kurento.kmf.repository;

public class HttpSessionErrorEvent extends RepositoryHttpSessionEvent {

	private String description;
	private Throwable cause;

	public HttpSessionErrorEvent(RepositoryHttpEndpoint source,
			String description) {
		super(source);
	}

	public HttpSessionErrorEvent(RepositoryHttpEndpoint source, Throwable cause) {
		super(source);
		this.cause = cause;
		this.description = cause.getMessage();
	}

	public Throwable getCause() {
		return cause;
	}

	public String getDescription() {
		return description;
	}

}
