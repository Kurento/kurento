com/kurento/kmf/media/AbstractBuilder.java
package com.kurento.kmf.media;

public interface AbstractBuilder<T> {

	public T build();

	public void buildAsync(Continuation<T> continuation);

}
