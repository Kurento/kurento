package org.kurento.client.internal.test.model.client;

import org.kurento.client.Continuation;

public interface AbstractBuilder<T> {

	public T build();

	public void buildAsync(Continuation<T> continuation);

}
