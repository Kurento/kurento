package com.kurento.tool.rom.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.jsonrpcconnector.Props;

public class RomClientObjectManager implements RomEventHandler {

	private static final Logger LOG = LoggerFactory
			.getLogger(RomClientObjectManager.class);

	private ConcurrentMap<String, RemoteObject> objects = new ConcurrentHashMap<>();

	@Override
	public void processEvent(String objectRef, String subscription,
			String type, Props data) {

		RemoteObject object = objects.get(objectRef);

		if (object == null) {
			LOG.error("Trying to fire event to an object that doesn't exist in the client");
			return;
		}

		object.fireEvent(type, data);
	}

	public void registerObject(String objectRef, RemoteObject remoteObject) {
		this.objects.put(objectRef, remoteObject);
	}

	public void releaseObject(String objectRef) {
		this.objects.remove(objectRef);
	}

}
