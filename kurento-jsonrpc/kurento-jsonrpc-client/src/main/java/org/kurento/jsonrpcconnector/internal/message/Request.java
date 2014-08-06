/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.jsonrpcconnector.internal.message;

/**
 * 
 * Java representation for JSON RPC request. This class holds the information
 * needed to invoke a method on the server
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * @param <P>
 */
public class Request<P> extends Message {

	public static final String METHOD_FIELD_NAME = "method";

	public static final String POLL_METHOD_NAME = "poll";

	/**
	 * Request identifier.
	 */
	private Integer id;

	/**
	 * Method to be invoked on the server
	 */
	protected String method;

	/**
	 * Method parameters
	 */
	protected P params;

	/**
	 * Default constructor.
	 */
	public Request() {
	}

	/**
	 * Parameterized constructor.
	 * 
	 * @param method
	 *            Thrift interface method
	 * @param params
	 *            Method parameters
	 * @param id
	 *            Request identifier
	 * @param sessionId
	 *            The session id associated to this request
	 */
	public Request(String sessionId, Integer id, String method, P params) {
		this.sessionId = sessionId;
		this.method = method;
		this.params = params;
		this.id = id;
	}

	/**
	 * Parameterized constructor.
	 * 
	 * @param method
	 *            Thrift interface method
	 * @param params
	 *            Method parameters
	 * @param id
	 *            Request identifier
	 */
	public Request(Integer id, String method, P params) {
		this(null, id, method, params);
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public P getParams() {
		return params;
	}

	public void setParams(P params) {
		this.params = params;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

}