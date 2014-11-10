package org.kurento.client.internal;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.kurento.client.TFuture;
import org.kurento.client.TransactionExecutionException;
import org.kurento.client.TransactionRollbackException;
import org.kurento.client.internal.client.operation.Operation;
import org.kurento.client.internal.server.KurentoServerException;
import org.kurento.commons.exception.KurentoException;

import com.google.common.util.concurrent.SettableFuture;

public class TFutureImpl<V> implements TFuture<V> {

	private SettableFuture<V> future;
	private Operation operation;

	public TFutureImpl(Operation operation) {
		this.future = SettableFuture.create();
		this.operation = operation;
	}

	@Override
	public boolean isRollback() {
		return future.isCancelled();
	}

	@Override
	public boolean isCommitted() {
		return future.isDone();
	}

	@Override
	public V get() {
		try {
			return future.get();
		} catch (InterruptedException e) {
			throw new KurentoException(e);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof KurentoServerException) {
				throw new TransactionExecutionException(operation,
						((KurentoServerException) e.getCause()).getError());
			} else {
				throw new KurentoException(e.getCause());
			}
		} catch (CancellationException e) {
			throw new TransactionRollbackException();
		}
	}

	public SettableFuture<V> getFuture() {
		return future;
	}

}
