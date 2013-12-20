package com.kurento.kmf.jsonrpcconnector.client;

import com.kurento.kmf.jsonrpcconnector.internal.message.ResponseError;

public class JsonRpcErrorException extends RuntimeException {

	private static final long serialVersionUID = 1584953670536766280L;

	private ResponseError error;

	public JsonRpcErrorException(ResponseError error) {
		super(error.getMessage());
		this.error = error;
	}

	public ResponseError getError() {
		return error;
	}

	public Object getData() {
		return error.getData();
	}

	public Object getCode() {
		return error.getCode();
	}

}
