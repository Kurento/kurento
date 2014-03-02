package com.kurento.tool.rom.test.model.client;

import com.kurento.kmf.media.Continuation;

public interface AbstractBuilder<T> {

	public T build();

	public void buildAsync(Continuation<T> continuation);

}
