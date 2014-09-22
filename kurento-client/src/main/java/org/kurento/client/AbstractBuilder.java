/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes shoult go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.client;

import org.kurento.client.internal.TransactionImpl;
import org.kurento.client.internal.client.NonCommitedRemoteObject;
import org.kurento.client.internal.client.RemoteObjectFacade;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.client.operation.MediaObjectCreationOperation;
import org.kurento.jsonrpc.Prop;
import org.kurento.jsonrpc.Props;

/**
 * Kurento Media Builder base interface
 *
 * Builds a {@code <T>} object, either synchronously using {@link #build} or
 * asynchronously using {@link #buildAsync}
 *
 * @param T
 *            the type of object to build
 *
 **/
public abstract class AbstractBuilder<T extends KurentoObject> {

	protected final Props props;
	private RomManager manager;
	private final Class<?> clazz;

	public AbstractBuilder(Class<?> clazz, MediaObject mediaObject) {

		this.props = new Props();
		this.clazz = clazz;
	}

	public AbstractBuilder(Class<?> clazz, RomManager manager) {

		this.props = new Props();
		this.clazz = clazz;
		this.manager = manager;
	}

	/**
	 * Builds an object synchronously using the builder design pattern
	 *
	 * @return <T> The type of object
	 *
	 **/
	public T create() {

		KurentoObject constObject = obtainConstructorObject();

		if (manager == null && constObject != null) {
			manager = constObject.getRomManager();
		}

		RemoteObjectFacade remoteObject = manager.create(clazz.getSimpleName(),
				props);

		return createMediaObjectConst(constObject, remoteObject, null);
	}

	public T create(Transaction transaction) {

		TransactionImpl tx = (TransactionImpl) transaction;

		NonCommitedRemoteObject remoteObject = new NonCommitedRemoteObject(
				tx.nextObjectRef(), tx);

		KurentoObject constObject = obtainConstructorObject();

		T mediaObject = createMediaObjectConst(constObject, remoteObject, tx);

		MediaObjectCreationOperation op = new MediaObjectCreationOperation(
				clazz.getSimpleName(), props, mediaObject);

		tx.addOperation(op);

		return mediaObject;
	}

	@SuppressWarnings("unchecked")
	private T createMediaObjectConst(KurentoObject constObject,
			RemoteObjectFacade remoteObject, Transaction tx) {

		KurentoObject mediaObject = createMediaObject(remoteObject, tx);

		if (constObject != null) {
			mediaObject.setInternalMediaPipeline(constObject
					.getInternalMediaPipeline());
		}

		return (T) mediaObject;
	}

	private KurentoObject obtainConstructorObject() {

		KurentoObject rootObject = null;
		for (Prop prop : props) {
			Object value = prop.getValue();
			if (value instanceof MediaPipeline || value instanceof Hub) {
				rootObject = (KurentoObject) value;
				break;
			}
		}
		return rootObject;
	}

	@Deprecated
	public T build() {
		return create();
	}

	protected abstract T createMediaObject(RemoteObjectFacade remoteObject,
			Transaction tx);

}
