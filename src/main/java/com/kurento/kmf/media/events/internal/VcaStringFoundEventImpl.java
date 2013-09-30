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
package com.kurento.kmf.media.events.internal;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.events.VcaStringFoundEvent;
import com.kurento.kmf.media.internal.ProvidesMediaEvent;
import com.kurento.kms.thrift.api.KmsEvent;
import com.kurento.kms.thrift.api.VcaStringFoundData;
import com.kurento.kms.thrift.api.mediaEventDataTypesConstants;

@ProvidesMediaEvent(type = mediaEventDataTypesConstants.VCA_STRING_FOUND)
public class VcaStringFoundEventImpl extends ThriftSerializedMediaEvent
		implements VcaStringFoundEvent {

	private String valueType;
	private String value;

	public VcaStringFoundEventImpl(KmsEvent event) {
		super(event);
	}

	@Override
	public String getValueType() {
		return valueType;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		VcaStringFoundData data = new VcaStringFoundData();
		try {
			data.read(pr);
		} catch (TException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000); // TODO
		}
		valueType = data.getValueType();
		value = data.getValue();
	}

}
