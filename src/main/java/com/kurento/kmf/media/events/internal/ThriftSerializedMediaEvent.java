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

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.apache.thrift.transport.TTransportException;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kms.thrift.api.KmsEvent;

public abstract class ThriftSerializedMediaEvent extends AbstractMediaEvent {

	public ThriftSerializedMediaEvent(KmsEvent event) {
		super(event);
	}

	@Override
	public void deserializeData(KmsEvent event) {
		if (event.isSetData()) {
			TMemoryBuffer tr = new TMemoryBuffer(event.data.remaining());
			TProtocol pr = new TBinaryProtocol(tr);
			byte data[] = new byte[event.data.remaining()];
			try {
				event.data.get(data);
				tr.write(data);
				deserializeFromTProtocol(pr);
			} catch (TTransportException e) {
				// TODO change error code
				throw new KurentoMediaFrameworkException(e.getMessage(), e,
						30000);
			}
		}
	}

	protected abstract void deserializeFromTProtocol(TProtocol pr);

}
