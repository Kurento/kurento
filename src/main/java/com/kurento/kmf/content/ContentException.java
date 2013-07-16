package com.kurento.kmf.content;

public class ContentException extends Exception {

	private static final long serialVersionUID = 1L;

	public ContentException(String msg) {
		super(msg);
	}

	public ContentException(Throwable e) {
		super(e);
	}

	public ContentException(String msg, Throwable e) {
		super(msg, e);
	}

}
