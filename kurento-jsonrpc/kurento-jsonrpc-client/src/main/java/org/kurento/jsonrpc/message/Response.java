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
package org.kurento.jsonrpc.message;

/**
 *
 * Java representation for JSON media connector request.
 *
 * @author Ivan Gracia (igracia@gsyc.es)
 * @version 1.0.0
 * @param <R>
 */
public class Response<R> extends Message {

	/**
	 * Request identifier related to this response
	 */
	private Integer id;

	/**
	 * Method result
	 */
	private R result;

	/**
	 * Error produced executing method
	 */
	private ResponseError error;

	/**
	 * Default constructor.
	 */
	public Response() {
	}

	public Response(R result) {
		this(null, null, result);
	}

	public Response(ResponseError error) {
		this(null, null, error);
	}

	public Response(Integer id, R result) {
		this(null, id, result);
	}

	public Response(Integer id, ResponseError error) {
		this(null, id, error);
	}

	public Response(Integer id) {
		super(null);
		this.id = id;
	}

	public Response(String sessionId, Integer id, R result) {
		super(sessionId);
		this.id = id;
		this.result = result;
	}

	public Response(String sessionId, Integer id, ResponseError error) {
		super(sessionId);
		this.id = id;
		this.error = error;
	}

	public R getResult() {
		return result;
	}

	public void setResult(R result) {
		this.result = result;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public ResponseError getError() {
		return error;
	}

	public void setError(ResponseError error) {
		this.error = error;
	}

	public boolean isError() {
		return error != null;
	}
}
