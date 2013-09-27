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
