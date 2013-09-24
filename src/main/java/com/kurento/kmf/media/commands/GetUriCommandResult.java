package com.kurento.kmf.media.commands;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.IsMediaCommandResult;

@IsMediaCommandResult(commandType = GetUriCommandResult.TYPE)
public class GetUriCommandResult extends ThriftSerializedCommandResult {

	public static final String TYPE = "GetUriCommandResult";

	private String uri;

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		try {
			uri = pr.readString();
		} catch (TException e) {
			// TODO: error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
	}

	public String getUri() {
		return uri;
	}
}
