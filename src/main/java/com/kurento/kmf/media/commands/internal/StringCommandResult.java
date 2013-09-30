package com.kurento.kmf.media.commands.internal;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;

public class StringCommandResult extends AbstractThriftSerializedCommandResult {

	private String result;

	public String getResult() {
		return this.result;
	}

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		try {
			result = pr.readString();
		} catch (TException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000); // TODO
		}
	}

}
