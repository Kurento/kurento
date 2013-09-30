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
import com.kurento.kmf.media.internal.ProvidesMediaEvent;
import com.kurento.kmf.media.internal.ZBarFilterImpl;
import com.kurento.kms.thrift.api.KmsEvent;

@ProvidesMediaEvent(type = ZBarEvent.TYPE)
public class ZBarEvent extends ThriftSerializedMediaEvent {

	// TODO Fix TYPE to something like StringEvent or other preconfigured event
	public static final String TYPE = "ZBarEvent";

	private String data;

	public ZBarEvent(KmsEvent event) {
		super(event);
	}

	@Override
	public ZBarFilterImpl getSource() {
		return (ZBarFilterImpl) super.getSource();
	}

	public String getData() {
		return data;
	}

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		try {
			data = pr.readString();
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
	}

}
