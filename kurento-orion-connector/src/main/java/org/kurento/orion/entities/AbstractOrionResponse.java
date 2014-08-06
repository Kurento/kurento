/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package org.kurento.orion.entities;

import com.google.gson.annotations.SerializedName;

/**
 * Abstract response having an {@link StatusCode} object, common to all Orion
 * responses. The only exception is the {@link ContextUpdateResponse}, which
 * holds a list of objects, each one of them having its own status code.
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 *
 */
class AbstractOrionResponse implements OrionResponse {

	@SerializedName("statusCode")
	private StatusCode statusCode;

	@Override
	public StatusCode getStatus() {
		return this.statusCode;
	}

	/**
	 * @param statusCode
	 *            the statusCode to set
	 */
	@Override
	public void setStatus(StatusCode statusCode) {
		this.statusCode = statusCode;
	}

	@Override
	public String toString() {
		return statusCode.toString();
	}
}
