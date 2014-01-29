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
package com.kurento.kmf.media.params.internal;

import static com.kurento.kms.thrift.api.KmsMediaPlayerEndPointTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.google.common.base.Objects;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.internal.ProvidesMediaParam;
import com.kurento.kms.thrift.api.KmsMediaPlayerEndPointConstructorParams;

/**
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 3.0.6
 *
 */
@ProvidesMediaParam(type = CONSTRUCTOR_PARAMS_DATA_TYPE)
public class PlayerEndpointConstructorParam extends
		AbstractThriftSerializedMediaParam {

	private Boolean useEncodedMedia;

	public Boolean getUseEncodedMedia() {
		return useEncodedMedia;
	}

	public void setUseEncodedMedia(Boolean useEncodedMedia) {
		this.useEncodedMedia = useEncodedMedia;
	}

	public PlayerEndpointConstructorParam() {
		super(CONSTRUCTOR_PARAMS_DATA_TYPE);
	}

	@Override
	protected TProtocol serializeDataToThrift(TProtocol pr) {
		KmsMediaPlayerEndPointConstructorParams kmsParams = new KmsMediaPlayerEndPointConstructorParams();

		if (this.useEncodedMedia != null) {
			kmsParams.setUseEncodedMedia(useEncodedMedia.booleanValue());
		}

		try {
			kmsParams.write(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
		return pr;
	}

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		KmsMediaPlayerEndPointConstructorParams kmsParams = new KmsMediaPlayerEndPointConstructorParams();

		try {
			kmsParams.read(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), 30000);
		}

		if (kmsParams.isSetUseEncodedMedia()) {
			this.useEncodedMedia = Boolean.valueOf(kmsParams.isUseEncodedMedia());
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

		PlayerEndpointConstructorParam param = (PlayerEndpointConstructorParam) obj;
		return Objects.equal(this.useEncodedMedia, param.useEncodedMedia);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.useEncodedMedia);
	}

}
