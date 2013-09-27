package com.kurento.kmf.media.commands.internal;

import com.kurento.kms.thrift.api.CommandResult;

public class DefaultMediaCommandResultImpl extends AbstractMediaCommandResult {

	private byte[] result;

	public DefaultMediaCommandResultImpl(String dataType) {
		super(dataType);
	}

	public byte[] getResult() {
		return result;
	}

	@Override
	public void deserializeCommandResult(CommandResult result) {
		this.result = result.getResult();
	}

}
