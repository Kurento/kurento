package com.kurento.kmf.repository;

public interface RepositoryHttpEventListener<E extends RepositoryHttpSessionEvent> {

	void onEvent(E event);

}
