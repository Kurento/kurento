package com.kurento.kmf.media.commands.internal;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TMemoryBuffer;

public abstract class AbstractThriftSerializedCommand extends
		AbstractMediaCommand {

	protected AbstractThriftSerializedCommand(String type) {
		super(type);
	}

	@Override
	protected byte[] getData() {
		TMemoryBuffer tr = new TMemoryBuffer(64); // default size. Will grow if
													// necessary
		TProtocol pr = new TBinaryProtocol(tr);
		return getThriftSerializedData(pr);
	}

	protected abstract byte[] getThriftSerializedData(TProtocol pr);

}
