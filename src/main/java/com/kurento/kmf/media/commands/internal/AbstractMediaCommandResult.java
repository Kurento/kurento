package com.kurento.kmf.media.commands.internal;

import com.kurento.kmf.media.commands.MediaCommandResult;
import com.kurento.kms.thrift.api.CommandResult;

public abstract class AbstractMediaCommandResult implements MediaCommandResult {

	public abstract void deserializeCommandResult(CommandResult result);

}
