${config.subfolder}/events/MediaEventListener.java
package ${config.packageName}.events;

public interface MediaEventListener<T extends Event> {

	void onEvent(T event);
}
