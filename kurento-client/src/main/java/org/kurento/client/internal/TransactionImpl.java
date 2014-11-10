package org.kurento.client.internal;

import java.util.ArrayList;
import java.util.List;

import org.kurento.client.Continuation;
import org.kurento.client.Transaction;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.client.operation.Operation;

public class TransactionImpl implements Transaction {

	private List<Operation> operations = new ArrayList<>();
	private RomManager manager;
	private int objectRef = 0;

	public TransactionImpl(RomManager manager) {
		this.manager = manager;
	}

	public void addOperation(Operation op) {
		this.operations.add(op);
	}

	public void commit() {
		manager.transaction(operations);
	}

	public void commit(Continuation<Void> continuation) {
		manager.transaction(operations, continuation);
	}

	public String nextObjectRef() {
		return "newref:" + (objectRef++);
	}

	@Override
	public void rollback() {
		for (Operation op : operations) {
			op.rollback(null);
		}
	}
}
