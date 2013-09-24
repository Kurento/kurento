package com.kurento.kmf.media.commands;

import com.kurento.kms.thrift.api.CommandResult;

public abstract class MediaCommandResult {
	public abstract void deserializeCommandResult(CommandResult result);
}
