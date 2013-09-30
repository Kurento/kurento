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
import com.kurento.kmf.media.events.HttpRequestReceived;
import com.kurento.kmf.media.internal.ProvidesMediaElement;
import com.kurento.kms.thrift.api.HttpRequestReceivedData;
import com.kurento.kms.thrift.api.KmsEvent;
import com.kurento.kms.thrift.api.mediaEventDataTypesConstants;

@ProvidesMediaElement(type = mediaEventDataTypesConstants.HTTP_REQUEST_RECEIVED)
public class HttpRequestReceivedImpl extends ThriftSerializedMediaEvent
		implements HttpRequestReceived {

	private HttpRequestReceivedData data;

	public HttpRequestReceivedImpl(KmsEvent event) {
		super(event);
	}

	@Override
	public HttpRequestReceivedData getData() {
		return data;
	}

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		try {
			data.read(pr);
		} catch (TException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000); // TODO
		}
	}

}
