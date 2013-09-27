package com.kurento.kmf.media.commands.internal;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;

public class StringCommand extends AbstractThriftSerializedCommand {

	private String data;

	public StringCommand(String type, String data) {
		super(type);
	}

	@Override
	protected byte[] getThriftSerializedData(TProtocol pr) {
		try {
			pr.writeString(data);
			byte[] buf = new byte[pr.getTransport().getBytesRemainingInBuffer()];
			pr.getTransport().read(buf, 0,
					pr.getTransport().getBytesRemainingInBuffer());
			return buf;
		} catch (TException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000); // TODO:
																				// error
																				// code
		}
	}
}
