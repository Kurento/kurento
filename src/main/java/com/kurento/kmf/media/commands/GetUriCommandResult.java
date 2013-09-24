package com.kurento.kmf.media.commands;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;

//TODO: add annotation
public class GetUriCommandResult extends ThriftSerializedCommandResult {

	private String uri;

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		try {
			uri = pr.readString();
		} catch (TException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000); // TODO:
																				// error
																				// code
		}
	}

	public String getUri() {
		return uri;
	}
}
