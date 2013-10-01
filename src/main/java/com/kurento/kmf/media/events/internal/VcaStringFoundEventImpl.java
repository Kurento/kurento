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

import static com.kurento.kms.thrift.api.ZBarFilterTypeConstants.EVENT_CODE_FOUND;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.events.VcaStringFoundEvent;
import com.kurento.kmf.media.internal.ProvidesMediaEvent;
import com.kurento.kms.thrift.api.EventCodeFoundData;
import com.kurento.kms.thrift.api.KmsEvent;

@ProvidesMediaEvent(type = EVENT_CODE_FOUND)
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
		EventCodeFoundData data = new EventCodeFoundData();
		try {
			data.read(pr);
		} catch (TException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000); // TODO
		}
		valueType = data.getType();
		value = data.getValue();
	}

}
