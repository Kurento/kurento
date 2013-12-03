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
package com.kurento.kmf.media.events;

import static com.kurento.kmf.media.internal.refs.MediaRefConverter.fromThrift;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.internal.refs.MediaObjectRef;
import com.kurento.kms.thrift.api.KmsMediaError;

/**
 * Error sent from the media server, during normal execution of a media element.
 * This error structure does not represent errors produced during the invocation
 * of a command, which are communicated to the caller as a
 * {@link KurentoMediaFrameworkException}.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public final class MediaError {

	private final MediaObjectRef objectRef;

	private final String description;

	private final Integer errorCode;

	private final String type;

	/**
	 * Constructor based on an error received from the media server.
	 * 
	 * @param error
	 *            The error structure received form the media server.
	 */
	public MediaError(KmsMediaError error) {
		this.type = error.type;
		this.description = error.description;
		this.errorCode = Integer.valueOf(error.errorCode);
		this.objectRef = fromThrift(error.source);
	}

	/**
	 * The reference to the object that produced the error.
	 * 
	 * @return The object's reference
	 */
	public MediaObjectRef getObjectRef() {
		return this.objectRef;
	}

	/**
	 * The description of the error.
	 * 
	 * @return The description.
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * The code for the error produced.
	 * 
	 * @return The error code.
	 */
	public Integer getErrorCode() {
		return this.errorCode;
	}

	/**
	 * The type of error produced.
	 * 
	 * @return The type of error.
	 */
	public String getType() {
		return this.type;
	}

}
