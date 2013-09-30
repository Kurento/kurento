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

import com.kurento.kmf.media.internal.refs.MediaObjectRefDTO;
import com.kurento.kms.thrift.api.KmsError;

public final class MediaError {

	private final MediaObjectRefDTO objectRef;

	private final String description;

	private final Integer errorCode;

	private final String type;

	public MediaError(KmsError error) {
		this.type = error.type;
		this.description = error.description;
		this.errorCode = Integer.valueOf(error.errorCode);
		this.objectRef = fromThrift(error.source);
	}

	public MediaObjectRefDTO getObjectRef() {
		return this.objectRef;
	}

	public String getDescription() {
		return this.description;
	}

	public Integer getErrorCode() {
		return this.errorCode;
	}

	public String getType() {
		return this.type;
	}
}
