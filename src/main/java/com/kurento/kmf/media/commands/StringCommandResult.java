package com.kurento.kmf.media.commands;

import com.kurento.kmf.media.IsMediaCommandResult;
import com.kurento.kms.thrift.api.CommandResult;

@IsMediaCommandResult(dataType = StringCommandResult.TYPE)
public class StringCommandResult extends MediaCommandResult {

	public static final String TYPE = "StringCommandResult";

	private String result;

	StringCommandResult() {
		super(TYPE);
	}

	public String getString() {
		return this.result;
	}

	@Override
	public void deserializeCommandResult(CommandResult commandResult) {
		this.result = commandResult.getResult().toString();
	}

}
