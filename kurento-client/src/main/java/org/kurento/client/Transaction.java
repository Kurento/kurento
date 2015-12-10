package org.kurento.client;

public interface Transaction {

	public void commit();

	public void commit(Continuation<Void> continuation);

	public void rollback();

}
