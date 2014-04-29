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

package com.kurento.kmf.repository;

/**
 * This class represents an event fired when an error is detected in the
 * {@link RepositoryHttpEndpoint} identified as source.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 */
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

	/**
	 * Returns the exception that caused this error or null if the error is not
	 * produced by means of an exception
	 * 
	 * @return the exception or null if the error is not produced by means of an
	 *         exception
	 */
	public Throwable getCause() {
		return cause;
	}

	/**
	 * Returns the description of the error. This description can be used to log
	 * the problem.
	 * 
	 * @return the description of the error.
	 */
	public String getDescription() {
		return description;
	}

}
