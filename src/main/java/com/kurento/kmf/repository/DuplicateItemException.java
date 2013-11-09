package com.kurento.kmf.repository;

public class DuplicateItemException extends RuntimeException {

	private static final long serialVersionUID = 3515920000618086477L;

	public DuplicateItemException(String id) {
		super("There is an item with id=" + id);
	}

}
