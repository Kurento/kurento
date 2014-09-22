package org.kurento.client;

import org.kurento.client.internal.client.RemoteObjectFacade;

public class InternalInfoGetter {

	public static RemoteObjectFacade getRemoteObject(KurentoObject mediaObject) {
		return mediaObject.getRemoteObject();
	}

	public static MediaPipeline getInternalMediaPipeline(
			KurentoObject mediaObject) {
		return mediaObject.getInternalMediaPipeline();
	}

	public static void setRemoteObject(KurentoObject mediaObject,
			RemoteObjectFacade remoteObject) {
		mediaObject.setRemoteObject(remoteObject);
	}
}
