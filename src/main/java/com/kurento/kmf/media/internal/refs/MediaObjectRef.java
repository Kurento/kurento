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
package com.kurento.kmf.media.internal.refs;

import com.kurento.kms.thrift.api.KmsMediaObjectRef;

public abstract class MediaObjectRef {

	protected final KmsMediaObjectRef objectRef;

	/**
	 * This constructor is used to preserve immutability of
	 * {@link MediaObjectRef#objectId}. A defensive copy of the id takes place,
	 * in order to have a unique reference.
	 * 
	 * @param id
	 */
	protected MediaObjectRef(KmsMediaObjectRef ref) {
		this.objectRef = ref.deepCopy();
	}

	public abstract MediaObjectRef deepCopy();

	public Long getId() {
		return Long.valueOf(this.objectRef.getId());
	}

	public String getToken() {
		return this.objectRef.getToken();
	}

	public KmsMediaObjectRef getThriftRef() {
		return this.objectRef;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		if (!obj.getClass().equals(this.getClass())) {
			return false;
		}

		MediaObjectRef moRef = (MediaObjectRef) obj;
		return moRef.objectRef.id == this.objectRef.id;
	}

	@Override
	public int hashCode() {
		int result = 13;
		result = (result * 31 + (int) (this.objectRef.id ^ (this.objectRef.id >>> 32)));
		return result;
	}

}
