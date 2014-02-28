package com.kurento.tool.rom.client;

import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.kurento.kmf.jsonrpcconnector.Props;
import com.kurento.kmf.media.Continuation;
import com.kurento.tool.rom.server.RomException;

public class RemoteObject {

	private static Logger LOG = LoggerFactory.getLogger(RemoteObject.class);
	
	public interface EventListener {
		public void onEvent(String eventType, Props data);
	}

	private String objectRef;
	private RomClient client;
	private RomClientObjectManager manager;
	private String type;
	
	private Multimap<String, EventListener> listeners = Multimaps
			.synchronizedMultimap(ArrayListMultimap.<String, EventListener>create());	

	public RemoteObject(String objectRef, String type, RomClient client,
			RomClientObjectManager manager) {
		this.objectRef = objectRef;
		this.client = client;
		this.manager = manager;
		this.type = type;

		this.manager.registerObject(objectRef, this);
	}

	@SuppressWarnings("unchecked")
	public <E> E invoke(String method, Props params, Class<E> clazz)
			throws RomException {

		return (E) invoke(method, params, (Type) clazz);
	}
	
	public Object invoke(String method, Props params, Type type,
			Continuation<?> cont) throws RomException {
		
		return client.invoke(objectRef, method, params, type, cont);
	}

	public Object invoke(String method, Props params, Type type)
			throws RomException {

		return client.invoke(objectRef, method, params, type);
	}

	public void release() throws RomException {
		client.release(objectRef);

		this.manager.releaseObject(objectRef);
	}

	public ListenerSubscription addEventListener(String eventType,
			EventListener listener) {

		return addEventListener(eventType, null, listener);
	}
	
	public ListenerSubscription addEventListener(String eventType,
			Continuation<?> cont, EventListener listener) {
		
		String subscription = client.subscribe(objectRef, eventType, cont);

		listeners.put(eventType, listener);

		return new ListenerSubscription(subscription, eventType, listener);
	}


	public String getObjectRef() {
		return objectRef;
	}

	public void fireEvent(String type, Props data) {
		for (EventListener eventListener : this.listeners.get(type)) {
			try {
				eventListener.onEvent(type, data);
			} catch (Exception e) {
				LOG.error("Exception executing event listener",e);
			}
		}
	}

	public String getType() {
		return type;
	}

}
