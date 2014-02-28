package com.kurento.tool.rom.server;

public class RomException extends RuntimeException {

	private static final long serialVersionUID = -5927887099220799744L;

	public RomException(String message, Throwable cause) {
		super(message, cause);
	}

	public RomException(String message) {
		super(message);
	}

	public RomException(Throwable cause) {
		super(cause);
	}

}
