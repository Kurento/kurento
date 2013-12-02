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

import static com.kurento.kms.thrift.api.KmsMediaRecorderEndPointTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.internal.ProvidesMediaParam;
import com.kurento.kms.thrift.api.KmsMediaMuxer;
import com.kurento.kms.thrift.api.KmsMediaProfile;
import com.kurento.kms.thrift.api.KmsMediaRecoderEndPointConstructorParams;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
@ProvidesMediaParam(type = CONSTRUCTOR_PARAMS_DATA_TYPE)
public class RecorderEndpointConstructorParam extends
		AbstractThriftSerializedMediaParam {

	private KmsMediaMuxer mediaMuxer;

	public KmsMediaMuxer getMediaMuxer() {
		return this.mediaMuxer;
	}

	public void setMediaMuxer(KmsMediaMuxer mediaMuxer) {
		this.mediaMuxer = mediaMuxer;
	}

	public RecorderEndpointConstructorParam() {
		super(CONSTRUCTOR_PARAMS_DATA_TYPE);
	}

	@Override
	protected TProtocol serializeDataToThrift(TProtocol pr) {
		KmsMediaRecoderEndPointConstructorParams kmsParams = new KmsMediaRecoderEndPointConstructorParams();

		if (this.mediaMuxer != null) {
			kmsParams.profileType = new KmsMediaProfile(mediaMuxer);
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
		KmsMediaRecoderEndPointConstructorParams kmsParams = new KmsMediaRecoderEndPointConstructorParams();
		try {
			kmsParams.read(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), 30000);
		}

		if (kmsParams.isSetProfileType()) {
			this.mediaMuxer = kmsParams.profileType.getMediaMuxer();
		}
	}
}
