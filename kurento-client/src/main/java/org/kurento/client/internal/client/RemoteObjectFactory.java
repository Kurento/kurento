package org.kurento.client.internal.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kurento.client.Continuation;
import org.kurento.jsonrpc.Props;

public class RemoteObjectFactory {

	private static final Logger log = LoggerFactory
			.getLogger(RemoteObjectFactory.class);

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
						try {
							cont.onSuccess(new RemoteObject(objectRef,
									remoteClassName, client, manager));
						} catch (Exception e) {
							log.warn(
									"[Continuation] error invoking onSuccess implemented by client",
									e);
						}
					}

					@Override
					public void onError(Throwable cause) {
						try {
							cont.onError(cause);
						} catch (Exception e) {
							log.warn(
									"[Continuation] error invoking onError implemented by client",
									e);
						}
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
