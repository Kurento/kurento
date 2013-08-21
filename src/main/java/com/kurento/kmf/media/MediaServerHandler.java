package com.kurento.kmf.media;

import java.nio.BufferUnderflowException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kms.api.MediaEvent;

class MediaServerHandler {

	private static Logger log = LoggerFactory
			.getLogger(MediaServerHandler.class);

	private final int handlerId;

	private final ConcurrentHashMap<MediaElement, Set<MediaEventListener<? extends KmsEvent>>> mediaElementMap;
	private final ConcurrentHashMap<MediaPipeline, Set<MediaEventListener<? extends KmsEvent>>> mediaPipelineMap;

	// TODO handlerId should be, at least, a long and should be generated in a
	// criptographically strong manner.
	MediaServerHandler() {
		handlerId = new Random(System.nanoTime()).nextInt();
		mediaElementMap = new ConcurrentHashMap<MediaElement, Set<MediaEventListener<? extends KmsEvent>>>();
		mediaPipelineMap = new ConcurrentHashMap<MediaPipeline, Set<MediaEventListener<? extends KmsEvent>>>();
	}

	int getHandlerId() {
		return handlerId;
	}

	<T extends KmsEvent> MediaEventListener<T> addListener(
			MediaObject mediaObject, MediaEventListener<T> listener) {
		if (mediaObject instanceof MediaPipeline) {
			Set<MediaEventListener<? extends KmsEvent>> listeners = mediaPipelineMap
					.get(mediaObject);
			if (listeners == null) {
				listeners = new CopyOnWriteArraySet<MediaEventListener<? extends KmsEvent>>();
				mediaPipelineMap.put((MediaPipeline) mediaObject, listeners);
			}
			listeners.add(listener);
			return listener;
		} else if (mediaObject instanceof MediaElement) {
			Set<MediaEventListener<? extends KmsEvent>> listeners = mediaElementMap
					.get(mediaObject);
			if (listeners == null) {
				listeners = new CopyOnWriteArraySet<MediaEventListener<? extends KmsEvent>>();
				mediaElementMap.put((MediaElement) mediaObject, listeners);
			}
			listeners.add(listener);
			return listener;
		} else {
			throw new UnsupportedOperationException(
					"Cannot add listener to object of type "
							+ mediaObject.getClass().getSimpleName());
		}
	}

	<T extends KmsEvent> boolean removeListener(MediaObject mediaObject,
			MediaEventListener<T> listener) {
		if (mediaObject instanceof MediaPipeline) {
			Set<MediaEventListener<? extends KmsEvent>> listeners = mediaPipelineMap
					.get(mediaObject);
			if (listeners == null) {
				return false;
			}
			boolean removed = listeners.remove(listener);
			if (listeners.size() == 0) {
				mediaPipelineMap.remove(mediaObject);
			}
			return removed;
		} else if (mediaObject instanceof MediaElement) {
			Set<MediaEventListener<? extends KmsEvent>> listeners = mediaElementMap
					.get(mediaObject);
			if (listeners == null) {
				return false;
			}
			boolean removed = listeners.remove(listener);
			if (listeners.size() == 0) {
				mediaElementMap.remove(mediaObject);
			}
			return removed;
		} else {
			throw new UnsupportedOperationException(
					"Cannot remove listener from object of type "
							+ mediaObject.getClass().getSimpleName());
		}
	}

	boolean removeAllListeners(MediaObject mediaObject) {
		if (mediaObject instanceof MediaPipeline) {
			return mediaPipelineMap.remove(mediaObject) != null;
		} else if (mediaObject instanceof MediaElement) {
			return mediaElementMap.remove(mediaObject) != null;
		}
		return false;
	}

	void onError(KmsError error) {
		// TODO
	}

	void onEvent(KmsEvent event) {
		if (event.getSource() instanceof MediaPipeline) {
			fireEvent(mediaPipelineMap.get(event.getSource()), event);
		} else if (event.getSource() instanceof MediaElement) {
			fireEvent(mediaElementMap.get(event.getSource()), event);
		} else {
			IllegalArgumentException iae = new IllegalArgumentException(
					"Received event associated to unsupported source class "
							+ event.getSource().getClass());
			log.error(iae.getMessage());
			throw iae;
		}
	}

	private void fireEvent(
			Set<MediaEventListener<? extends KmsEvent>> listeners,
			KmsEvent event) {
		if (listeners == null) {
			return;
		}

		for (MediaEventListener<? extends KmsEvent> listener : listeners) {
			try {
				listener.internalOnEvent(event);
			} catch (Throwable t) {
				log.error(t.getMessage(), t);
			}
		}
	}

	TProtocol getProtocolFromEvent(MediaEvent event) throws TException {
		TMemoryBuffer tr = new TMemoryBuffer(event.event.remaining());
		TProtocol prot = new TBinaryProtocol(tr);

		byte data[] = new byte[event.event.remaining()];
		try {
			event.event.get(data);

			tr.write(data);

			return prot;
		} catch (BufferUnderflowException e) {
			log.error("Error deserializing event: " + e, e);
			throw new TException(e);
		}
	}
}
