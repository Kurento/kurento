package com.kurento.kmf.content.jsonrpc;

/**
 * 
 * Operations involved in the transfer of media.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Miguel París (mparisdiaz@gsyc.es)
 * @version 1.0.0
 */
public enum Constraints {
	/**
	 * Send only operation.
	 */
	SENDONLY,
	/**
	 * Receive only operation.
	 */
	RECVONLY,
	/**
	 * Send and receive operation.
	 */
	SENDRECV,
	/**
	 * No operation.
	 */
	INACTIVE
}
