package com.kurento.kmf.content;

/**
 * Exception class within the Content Management API.
 * 
 * @author Miguel París (mparisdiaz@gsyc.es)
 * 
 */
public class ContentException extends Exception {

	/**
	 * Default serial version ID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor with message of the error/exception.
	 * 
	 * @param msg
	 *            message of the error/exception
	 */
	public ContentException(String msg) {
		super(msg);
	}

	/**
	 * Constructor with the origin error/exception (Throwable).
	 * 
	 * @param e
	 *            Causing class of the error/exception
	 */
	public ContentException(Throwable e) {
		super(e);
	}

	/**
	 * Constructor with both the message and the origin error/exception
	 * (Throwable).
	 * 
	 * @param msg
	 *            message of the error/exception
	 * @param e
	 *            Causing class of the error/exception
	 */
	public ContentException(String msg, Throwable e) {
		super(msg, e);
	}

}
