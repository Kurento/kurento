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

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.apache.thrift.transport.TTransportException;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kms.thrift.api.KmsMediaParam;

public abstract class AbstractThriftSerializedMediaParam extends
		AbstractMediaParam {

	/**
	 * @param type
	 */
	protected AbstractThriftSerializedMediaParam(String type) {
		super(type);
	}

	@Override
	public String getDataType() {
		return this.dataType;
	}

	@Override
	protected byte[] getData() {
		TMemoryBuffer tr = new TMemoryBuffer(64);
		TProtocol pr = new TBinaryProtocol(tr);
		pr = this.getThriftSerializedData(pr);
		byte[] buf = new byte[pr.getTransport().getBytesRemainingInBuffer()];

		try {
			pr.getTransport().read(buf, 0,
					pr.getTransport().getBytesRemainingInBuffer());
		} catch (TTransportException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
		return buf;
	}

	@Override
	public void deserializeCommandResult(KmsMediaParam result) {
		if (result.isSetData()) {
			TMemoryBuffer tr = new TMemoryBuffer(result.data.remaining());
			TProtocol pr = new TBinaryProtocol(tr);
			byte data[] = new byte[result.data.remaining()];
			try {
				result.data.get(data);
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

	protected abstract TProtocol getThriftSerializedData(TProtocol pr);
}
