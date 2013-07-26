package com.kurento.kmf.media.internal;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

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

import com.kurento.kmf.media.MediaManagerHandler;
import com.kurento.kms.api.MediaServerService;

public class MediaServerServiceManager {

	private static Logger log = LoggerFactory
			.getLogger(MediaServerServiceManager.class);

	private static final int WARN_SIZE = 3;

	private final String address;
	private final int port;

	private final Set<MediaServerService.Client> mediaServerServicesInUse = new CopyOnWriteArraySet<MediaServerService.Client>();
	private final Set<MediaServerService.AsyncClient> mediaServerServicesAsyncInUse = new CopyOnWriteArraySet<MediaServerService.AsyncClient>();

	private static MediaServerServiceManager singleton = null;

	public static synchronized void init(String address, int port,
			MediaManagerHandler handler) throws IllegalStateException,
			IOException {
		MediaServerServiceManager manager;

		synchronized (MediaServerServiceManager.class) {
			if (singleton == null) {
				try {
					manager = new MediaServerServiceManager(address, port,
							handler);
				} catch (TTransportException e) {
					throw new IOException(e);
				}

				if (singleton == null)
					singleton = manager;
			} else {
				throw new IllegalStateException("Already initialized");
			}
		}
	}

	private MediaServerServiceManager(String address, int port,
			MediaManagerHandler handler) throws TTransportException {
		this.address = address;
		this.port = port;

		MediaServerService.Client server = getMediaServerService();
		// TODO:
		// server.sendClusterCodeForHandler(clusterCode, handlerAddr,
		// handlerPort);
		releaseMediaServerService(server);
	}

	public static synchronized MediaServerServiceManager getInstance()
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
		TTransport transport = new TFramedTransport(new TSocket(address, port));
		// TODO: Make protocol configurable
		TProtocol prot = new TBinaryProtocol(transport);
		transport.open();
		return new MediaServerService.Client(prot);
	}

	public MediaServerService.Client getMediaServerService()
			throws TTransportException {
		MediaServerService.Client service = createMediaServerService();
		mediaServerServicesInUse.add(service);

		if (mediaServerServicesInUse.size() > WARN_SIZE)
			log.warn("Number of serverService clients over warning size");

		return service;
	}

	public void releaseMediaServerService(MediaServerService.Client service) {
		mediaServerServicesInUse.remove(service);

		service.getInputProtocol().getTransport().close();
		service.getOutputProtocol().getTransport().close();
	}

	private MediaServerService.AsyncClient createMediaServerServiceAsync()
			throws IOException {
		TNonblockingTransport transport = new TNonblockingSocket(address, port);
		TAsyncClientManager clientManager = new TAsyncClientManager();
		TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
		return new MediaServerService.AsyncClient(protocolFactory,
				clientManager, transport);
	}

	public MediaServerService.AsyncClient getMediaServerServiceAsync()
			throws TTransportException, IOException {
		MediaServerService.AsyncClient service = createMediaServerServiceAsync();
		mediaServerServicesAsyncInUse.add(service);

		if (mediaServerServicesAsyncInUse.size() > WARN_SIZE)
			log.warn("Number of serverService clients over warning size");

		return service;
	}

	public void releaseMediaServerServiceAsync(
			MediaServerService.AsyncClient service) {
		mediaServerServicesAsyncInUse.remove(service);
	}

}
