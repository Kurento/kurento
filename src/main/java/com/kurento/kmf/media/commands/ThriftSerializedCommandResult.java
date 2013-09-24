package com.kurento.kmf.media.commands;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.apache.thrift.transport.TTransportException;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kms.thrift.api.CommandResult;

public abstract class ThriftSerializedCommandResult extends MediaCommandResult {

	ThriftSerializedCommandResult(String type) {
		super(type);
	}

	@Override
	public void deserializeCommandResult(CommandResult result) {
		TMemoryBuffer tr = new TMemoryBuffer(result.result.remaining());
		TProtocol pr = new TBinaryProtocol(tr);
		byte data[] = new byte[result.result.remaining()];
		try {
			result.result.get(data);
			tr.write(data);
			deserializeFromTProtocol(pr);
		} catch (TTransportException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
	}

	protected abstract void deserializeFromTProtocol(TProtocol pr);

}
