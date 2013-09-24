package com.kurento.kmf.media.events;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.apache.thrift.transport.TTransportException;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kms.thrift.api.MediaEvent;

public abstract class ThriftSerializedMediaEvent extends KmsEvent {

	public ThriftSerializedMediaEvent(MediaEvent event) {
		super(event);
	}

	@Override
	public void deserializeData(MediaEvent event) {
		TMemoryBuffer tr = new TMemoryBuffer(event.data.remaining());
		TProtocol pr = new TBinaryProtocol(tr);
		byte data[] = new byte[event.data.remaining()];
		try {
			event.data.get(data);
			tr.write(data);
			deserializeFromTProtocol(pr);
		} catch (TTransportException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000); // TODO
																				// code
		}

	}

	protected abstract void deserializeFromTProtocol(TProtocol pr);

}
