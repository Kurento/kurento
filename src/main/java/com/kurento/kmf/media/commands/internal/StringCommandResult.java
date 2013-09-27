package com.kurento.kmf.media.commands.internal;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.internal.ProvidesMediaCommandResult;
import com.kurento.kms.thrift.api.mediaCommandDataTypesConstants;

@ProvidesMediaCommandResult(dataType = mediaCommandDataTypesConstants.STRING_COMMAND_RESULT)
public class StringCommandResult extends AbstractThriftSerializedCommandResult {

	private String result;

	StringCommandResult() {
		super(mediaCommandDataTypesConstants.STRING_COMMAND_RESULT);
	}

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
