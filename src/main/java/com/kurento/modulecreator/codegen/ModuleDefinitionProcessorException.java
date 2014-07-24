package com.kurento.modulecreator.codegen;

public class ModuleDefinitionProcessorException extends RuntimeException {

	private static final long serialVersionUID = -5373430051337208460L;

	public ModuleDefinitionProcessorException() {
	}

	public ModuleDefinitionProcessorException(String message) {
		super(message);
	}

	public ModuleDefinitionProcessorException(Throwable cause) {
		super(cause);
	}

	public ModuleDefinitionProcessorException(String message, Throwable cause) {
		super(message, cause);
	}

	public ModuleDefinitionProcessorException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
