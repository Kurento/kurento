package com.kurento.kmf.media;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.thrift.TException;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TNonblockingTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;

class MediaServerServiceManager {

	private static Logger log = LoggerFactory
			.getLogger(MediaServerServiceManager.class);

	private static final int WARN_SIZE = 3;

	private final String serverAddress;
	private final int serverPort;
	private final MediaHandlerServer mediaHandlerServer;

	private final Set<MediaServerService.Client> mediaServerServicesInUse = new CopyOnWriteArraySet<MediaServerService.Client>();
	private final Set<MediaServerService.AsyncClient> mediaServerServicesAsyncInUse = new CopyOnWriteArraySet<MediaServerService.AsyncClient>();

	private static MediaServerServiceManager singleton = null;

	public static synchronized void init(String serverAddress, int serverPort,
			MediaManagerHandler handler, int handlerId, String handlerAddress,
			int handlerPort) throws IllegalStateException, IOException {

		synchronized (MediaServerServiceManager.class) {
			if (singleton == null) {
				singleton = new MediaServerServiceManager(serverAddress,
						serverPort, handler, handlerId, handlerAddress,
						handlerPort);

				MediaServerService.Client service = getMediaServerService();
				try {
					service.addHandlerAddress(handlerId, handlerAddress,
							handlerPort);
				} catch (MediaServerException e) {
					throw new IOException(e);
				} catch (TException e) {
					throw new IOException(e);
				} finally {
					releaseMediaServerService(service);
				}
			} else {
				throw new IllegalStateException("Already initialized");
			}
		}
	}

	public static synchronized void destroy() throws IllegalStateException {
		synchronized (MediaServerServiceManager.class) {
			if (singleton != null) {
				singleton.stopHandlerServer();
			} else {
				throw new IllegalStateException("Not initialized");
			}
		}
	}

	private MediaServerServiceManager(String serverAddress, int serverPort,
			MediaManagerHandler handler, int handlerId, String handlerAddress,
			int handlerPort) throws IOException {
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;

		mediaHandlerServer = new MediaHandlerServer(handlerPort, handler);
		mediaHandlerServer.start();
	}

	private synchronized void stopHandlerServer() {
		mediaHandlerServer.stop();
	}

	private static synchronized MediaServerServiceManager getInstance()
			throws IllegalStateException {
		if (singleton != null) {
			return singleton;
		} else {
			throw new IllegalStateException("Not initialized");
		}
	}

	private MediaServerService.Client createMediaServerService()
			throws TTransportException {
		// FIXME: use pool to avoid no such sockets
		TTransport transport = new TFramedTransport(new TSocket(serverAddress,
				serverPort));
		// TODO: Make protocol configurable
		TProtocol prot = new TBinaryProtocol(transport);
		transport.open();
		return new MediaServerService.Client(prot);
	}

	private MediaServerService.Client getMediaServerServiceInternal()
			throws TTransportException {
		MediaServerService.Client service = createMediaServerService();
		mediaServerServicesInUse.add(service);

		if (mediaServerServicesInUse.size() > WARN_SIZE)
			log.warn("Number of serverService clients over warning size");

		return service;
	}

	public static MediaServerService.Client getMediaServerService()
			throws IOException {
		MediaServerServiceManager manager = MediaServerServiceManager
				.getInstance();
		try {
			return manager.getMediaServerServiceInternal();
		} catch (TTransportException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	private void releaseMediaServerServiceInternal(
			MediaServerService.Client service) {
		mediaServerServicesInUse.remove(service);

		service.getInputProtocol().getTransport().close();
		service.getOutputProtocol().getTransport().close();
	}

	public static void releaseMediaServerService(
			MediaServerService.Client service) {
		MediaServerServiceManager manager = MediaServerServiceManager
				.getInstance();
		manager.releaseMediaServerServiceInternal(service);
	}

	private MediaServerService.AsyncClient createMediaServerServiceAsync()
			throws IOException {
		TNonblockingTransport transport = new TNonblockingSocket(serverAddress,
				serverPort);
		TAsyncClientManager clientManager = new TAsyncClientManager();
		TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
		return new MediaServerService.AsyncClient(protocolFactory,
				clientManager, transport);
	}

	private MediaServerService.AsyncClient getMediaServerServiceAsyncInternal()
			throws IOException {
		MediaServerService.AsyncClient service = createMediaServerServiceAsync();
		mediaServerServicesAsyncInUse.add(service);

		if (mediaServerServicesAsyncInUse.size() > WARN_SIZE)
			log.warn("Number of serverService clients over warning size");

		return service;
	}

	public static MediaServerService.AsyncClient getMediaServerServiceAsync()
			throws IOException {
		MediaServerServiceManager manager = MediaServerServiceManager
				.getInstance();
		return manager.getMediaServerServiceAsyncInternal();
	}

	private void releaseMediaServerServiceAsyncInternal(
			MediaServerService.AsyncClient service) {
		mediaServerServicesAsyncInUse.remove(service);
	}

	public static void releaseMediaServerServiceAsync(
			MediaServerService.AsyncClient service) {
		MediaServerServiceManager manager = MediaServerServiceManager
				.getInstance();
		manager.releaseMediaServerServiceAsyncInternal(service);
	}

}
