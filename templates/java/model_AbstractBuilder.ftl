${config.subfolder}/AbstractBuilder.java
package ${config.packageName};

public interface AbstractBuilder<T> {

	public T build();

	public void buildAsync(Continuation<T> continuation);

}
