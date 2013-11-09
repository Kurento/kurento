package com.kurento.kmf.repository;

public class RepositoryHttpSessionEvent {

	private RepositoryHttpEndpoint source;

	public RepositoryHttpSessionEvent(RepositoryHttpEndpoint source) {
		this.source = source;
	}

	public RepositoryHttpEndpoint getSource() {
		return source;
	}
}
