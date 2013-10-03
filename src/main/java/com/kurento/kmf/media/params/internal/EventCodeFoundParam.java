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

import static com.kurento.kms.thrift.api.KmsMediaZBarFilterTypeConstants.EVENT_CODE_FOUND_DATA_TYPE;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.internal.ProvidesMediaParam;
import com.kurento.kms.thrift.api.KmsMediaEventCodeFoundData;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
@ProvidesMediaParam(type = EVENT_CODE_FOUND_DATA_TYPE)
public class EventCodeFoundParam extends AbstractThriftSerializedMediaParam {

	private String codeType;

	private String value;

	public String getCodeType() {
		return codeType;
	}

	public void setCookieLifetime(String codeType) {
		this.codeType = codeType;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public EventCodeFoundParam() {
		super(EVENT_CODE_FOUND_DATA_TYPE);
	}

	@Override
	protected TProtocol getThriftSerializedData(TProtocol pr) {
		KmsMediaEventCodeFoundData kmsParams = new KmsMediaEventCodeFoundData();
		if (codeType != null) {
			kmsParams.setType(codeType);
		}

		if (value != null) {
			kmsParams.setValue(value);
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
		KmsMediaEventCodeFoundData kmsParams = new KmsMediaEventCodeFoundData();
		try {
			kmsParams.read(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), 30000);
		}

		if (kmsParams.isSetType()) {
			this.codeType = kmsParams.getType();
		}

		if (kmsParams.isSetValue()) {
			this.value = kmsParams.getValue();
		}
	}

}
