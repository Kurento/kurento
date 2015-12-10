package org.kurento.client;

import org.kurento.client.internal.client.operation.Operation;
import org.kurento.client.internal.server.KurentoServerException;
import org.kurento.jsonrpc.message.ResponseError;

public class TransactionExecutionException extends KurentoServerException {

	private static final long serialVersionUID = 6694105597823767195L;

	public TransactionExecutionException(Operation operation,
			ResponseError error) {
		super(createExceptionMessage(operation, error), error);
	}

	private static String createExceptionMessage(Operation operation,
			ResponseError error) {
		return "Error '" + error.getCompleteMessage()
				+ "' executing operation '" + operation.getDescription() + "'";
	}

}
