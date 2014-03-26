package com.kurento.tool.rom.client;

import com.kurento.kmf.jsonrpcconnector.Props;
import com.kurento.kmf.media.Continuation;

public class RemoteObjectFactory {

	private final RomClientObjectManager manager;
	private final RomClient client;

	public RemoteObjectFactory(RomClient client) {
		this.client = client;
		this.manager = new RomClientObjectManager(client);
		this.client.addRomEventHandler(manager);
	}

	public RemoteObject create(String remoteClassName, Props constructorParams) {

		String objectRef = client.create(remoteClassName, constructorParams);

		return new RemoteObject(objectRef, remoteClassName, client, manager);
	}

	public RemoteObject create(String remoteClassName) {
		return create(remoteClassName, (Props) null);
	}

	public void create(final String remoteClassName,
			final Props constructorParams, final Continuation<RemoteObject> cont) {

		client.create(remoteClassName, constructorParams,
				new Continuation<String>() {
					@Override
					public void onSuccess(String objectRef) {
						cont.onSuccess(new RemoteObject(objectRef,
								remoteClassName, client, manager));
					}

					@Override
					public void onError(Throwable cause) {
						cont.onError(cause);
					}
				});
	}

	public void create(String remoteClassName, Continuation<RemoteObject> cont) {
		create(remoteClassName, null, cont);
	}

	public void destroy() {
		this.client.destroy();
	}
}
