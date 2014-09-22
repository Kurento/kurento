package org.kurento.client.internal.client;

import java.lang.reflect.Type;
import java.util.List;

import org.kurento.client.Continuation;
import org.kurento.client.internal.client.operation.Operation;
import org.kurento.client.internal.server.KurentoServerException;
import org.kurento.client.internal.transport.serialization.ObjectRefsManager;
import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RomManager implements ObjectRefsManager {

	private static final String METHOD_NOT_FOUND_ERROR_MSG = "Method not found.";

	private static final Logger log = LoggerFactory.getLogger(RomManager.class);

	private final RomClientObjectManager manager;
	private final RomClient client;

	private boolean serverTransactionSupport = true;

	public RomManager(RomClient client) {
		this.client = client;
		this.manager = new RomClientObjectManager(this);
		if (client != null) {
			this.client.addRomEventHandler(manager);
		}
	}

	public RemoteObject create(String remoteClassName, Props constructorParams) {
		String objectRef = client.create(remoteClassName, constructorParams);
		return new RemoteObject(objectRef, remoteClassName, this);
	}

	public RemoteObject create(String remoteClassName) {
		return create(remoteClassName, (Props) null);
	}

	public void create(final String remoteClassName,
			final Props constructorParams,
			final Continuation<RemoteObjectFacade> cont) {

		client.create(remoteClassName, constructorParams,
				new Continuation<String>() {
					@Override
					public void onSuccess(String objectRef) {
						try {
							cont.onSuccess(new RemoteObject(objectRef,
									remoteClassName, RomManager.this));
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

	public void create(String remoteClassName,
			Continuation<RemoteObjectFacade> cont) {
		create(remoteClassName, null, cont);
	}

	public void destroy() {
		this.client.destroy();
	}

	public <E> E invoke(String objectRef, String methodName, Props params,
			Class<E> clazz) {
		return client.invoke(objectRef, methodName, params, clazz);
	}

	public Object invoke(String objectRef, String operationName,
			Props operationParams, Type type) {
		return client.invoke(objectRef, operationName, operationParams, type);
	}

	public void release(String objectRef) {
		client.release(objectRef);
		manager.releaseObject(objectRef);
	}

	public String subscribe(String objectRef, String eventType) {
		return client.subscribe(objectRef, eventType);
	}

	public Object invoke(String objectRef, String operationName,
			Props operationParams, Type type, Continuation<?> cont) {
		return client.invoke(objectRef, operationName, operationParams, type,
				cont);
	}

	public void release(final String objectRef, final Continuation<Void> cont) {
		client.release(objectRef, new DefaultContinuation<Void>(cont) {
			@Override
			public void onSuccess(Void result) {
				manager.releaseObject(objectRef);
				try {
					cont.onSuccess(null);
				} catch (Exception e) {
					log.warn(
							"[Continuation] error invoking onSuccess implemented by client",
							e);
				}
			}
		});
	}

	public String subscribe(String objectRef, String type,
			Continuation<String> cont) {
		return client.subscribe(objectRef, type, cont);
	}

	public void addRomEventHandler(RomEventHandler eventHandler) {
		client.addRomEventHandler(eventHandler);
	}

	@Override
	public Object getObject(String objectRef) {
		return manager.getObject(objectRef);
	}

	public void registerObject(String objectRef, RemoteObject remoteObject) {
		this.manager.registerObject(objectRef, remoteObject);
	}

	public RomClientObjectManager getObjectManager() {
		return manager;
	}

	public void transaction(List<Operation> operations) {
		for (Operation op : operations) {
			op.setManager(this);
		}
		if (serverTransactionSupport) {
			try {
				client.transaction(operations);
			} catch (KurentoServerException e) {

				Throwable cause = e.getCause();
				if (cause instanceof JsonRpcErrorException
						&& cause.getMessage()
								.equals(METHOD_NOT_FOUND_ERROR_MSG)) {
					serverTransactionSupport = false;
					transaction(operations);

				} else {
					throw e;
				}
			}

		} else {
			log.debug("Start transaction execution");
			// TODO Improve error handling and sort operations in smart way
			for (Operation operation : operations) {
				operation.exec(this);
			}
			log.debug("End transaction execution");
		}
	}

	// TODO Improve this async exec. Review error handling
	// TODO Improve error handling and sort operations in smart way
	public void transaction(final List<Operation> operations,
			final Continuation<Void> continuation) {
		for (Operation op : operations) {
			op.setManager(this);
		}
		if (serverTransactionSupport) {
			client.transaction(operations, new Continuation<Void>() {
				@Override
				public void onSuccess(Void result) throws Exception {
					continuation.onSuccess(null);
				}

				@Override
				public void onError(Throwable cause) throws Exception {
					// Retry one more time (Daft Punk)
					RomManager.this.serverTransactionSupport = false;
					transaction(operations, continuation);
				}
			});
		} else {
			log.debug("Start transaction async execution");
			transactionOperationRec(operations, continuation);
		}
	}

	private void transactionOperationRec(final List<Operation> operations,
			final Continuation<Void> cont) {
		if (operations.size() > 0) {
			Operation op = operations.remove(0);

			op.exec(this, new DefaultContinuation<Void>(cont) {
				public void onSuccess(Void result) throws Exception {
					if (operations.size() > 0) {
						transactionOperationRec(operations, cont);
					} else {
						log.debug("End transaction async execution");
						cont.onSuccess(null);
					}
				}
			});
		}
	}

	public RomClient getRomClient() {
		return client;
	}

}
