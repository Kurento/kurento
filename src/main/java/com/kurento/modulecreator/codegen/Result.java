package com.kurento.modulecreator.codegen;

import java.util.Arrays;
import java.util.List;

public class Result {

	private boolean success;
	private List<Error> errors;

	public Result() {
		this.success = true;
	}

	public Result(List<Error> errors) {
		this.errors = errors;
		this.success = false;
	}

	public Result(Error... errors) {
		this(Arrays.asList(errors));
	}

	public boolean isSuccess() {
		return success;
	}

	public void showErrorsInConsole() {
		System.out.println("Errors:");
		for (Error error : errors) {
			System.out.println(error);
		}
	}

	public List<Error> getErrors() {
		return errors;
	}
}
