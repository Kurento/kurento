package com.kurento.tool.rom.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.jsonrpcconnector.Props;
import com.kurento.tool.rom.transport.serialization.ObjectRefsManager;

public class RomClientObjectManager implements RomEventHandler,
		ObjectRefsManager {

	private static final Logger LOG = LoggerFactory
			.getLogger(RomClientObjectManager.class);

	private DistributedGarbageCollector dgc;

	private ConcurrentMap<String, RemoteObject> objects = new ConcurrentHashMap<>();

	private RomClient client;

	public RomClientObjectManager(RomClient client) {
		this.client = client;
		this.dgc = new DistributedGarbageCollector(client);
	}

	public RomClient getClient() {
		return client;
	}

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
		// this.dgc.registerReference(objectRef);
	}

	public void releaseObject(String objectRef) {
		this.objects.remove(objectRef);
		// this.dgc.removeReference(objectRef);
	}

	@Override
	public Object getObject(String objectRef) {
		return this.objects.get(objectRef);
	}

}
