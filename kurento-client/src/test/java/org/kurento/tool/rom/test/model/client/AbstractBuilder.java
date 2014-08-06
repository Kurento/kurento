package org.kurento.tool.rom.test.model.client;

import org.kurento.media.Continuation;

public interface AbstractBuilder<T> {

	public T build();

	public void buildAsync(Continuation<T> continuation);

}
