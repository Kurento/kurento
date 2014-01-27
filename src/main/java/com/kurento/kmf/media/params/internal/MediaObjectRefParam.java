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
package com.kurento.kmf.media.params.internal;

import static com.kurento.kms.thrift.api.KmsMediaServerConstants.MEDIA_OBJECT_REF;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.internal.ProvidesMediaParam;
import com.kurento.kmf.media.internal.refs.MediaObjectRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kms.thrift.api.KmsMediaObjectRef;

/**
 * This {@link MediaParam}
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 * @since 2.0.1
 * 
 */
@ProvidesMediaParam(type = MEDIA_OBJECT_REF)
public final class MediaObjectRefParam extends
		AbstractThriftSerializedMediaParam {

	private KmsMediaObjectRef kmsRef;

	/**
	 * Constructor to be used by the framework.
	 */
	public MediaObjectRefParam() {
		super(MEDIA_OBJECT_REF);
	}

	/**
	 * Constructor with parameters.
	 * 
	 * @param objRef
	 * 
	 */
	public MediaObjectRefParam(MediaObjectRef objRef) {
		this();
		this.kmsRef = objRef.getThriftRef().deepCopy();
	}

	@Override
	protected TProtocol serializeDataToThrift(TProtocol pr) {

		try {
			this.kmsRef.write(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
		return pr;
	}

	@Override
	protected void deserializeFromTProtocol(final TProtocol pr) {
		this.kmsRef = new KmsMediaObjectRef();
		try {
			this.kmsRef.read(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), 30000);
		}
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

		MediaObjectRefParam param = (MediaObjectRefParam) obj;
		return this.kmsRef.equals(param.kmsRef);
	}

	@Override
	public int hashCode() {
		return this.kmsRef.hashCode();
	}

}
