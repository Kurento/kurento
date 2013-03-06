package com.kurento.kms.media.internal;

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

import com.kurento.kms.api.MediaServerService;

public class MediaServerServiceManager {

	private static Logger log = LoggerFactory
			.getLogger(MediaServerServiceManager.class);

	private static final int WARN_SIZE = 3;

	private String address;
	private int port;

	private Set<MediaServerService.Client> mediaServerServicesInUse = new CopyOnWriteArraySet<MediaServerService.Client>();
	private Set<MediaServerService.AsyncClient> mediaServerServicesAsyncInUse = new CopyOnWriteArraySet<MediaServerService.AsyncClient>();

	private static MediaServerServiceManager singleton = null;

	public static synchronized void init(String address, int port)
			throws TException {
		MediaServerServiceManager manager;
		boolean create = false;

		synchronized (MediaServerServiceManager.class) {
			create = singleton == null;
		}

		if (create) {
			manager = new MediaServerServiceManager(address, port);

			synchronized (MediaServerServiceManager.class) {
				if (singleton == null)
					singleton = manager;
			}
		}
	}

	private MediaServerServiceManager(String address, int port)
			throws TException {
		this.address = address;
		this.port = port;

		// TODO: is it necessary?
		MediaServerService.Client server = getMediaServerService();
		releaseMediaServerService(server);
	}

	public static synchronized MediaServerServiceManager getInstance()
			throws TException {
		if (singleton != null) {
			return singleton;
		} else {
			throw new TException("Services manager was not initialized");
		}
	}

	public MediaServerService.Client getMediaServerService()
			throws TTransportException {
		MediaServerService.Client service = createMediaServerService();
		mediaServerServicesInUse.add(service);

		if (mediaServerServicesInUse.size() > WARN_SIZE)
			log.warn("Numer of serverService clients over warning size");

		return service;
	}

	public void releaseMediaServerService(MediaServerService.Client service) {
		mediaServerServicesInUse.remove(service);

		service.getInputProtocol().getTransport().close();
		service.getOutputProtocol().getTransport().close();
	}

	private MediaServerService.Client createMediaServerService()
			throws TTransportException {
		TTransport transport = new TFramedTransport(new TSocket(address, port));
		// TODO: Make protocol configurable
		TProtocol prot = new TBinaryProtocol(transport);
		transport.open();
		return new MediaServerService.Client(prot);
	}

	public MediaServerService.AsyncClient getMediaServerServiceAsync()
			throws TTransportException, IOException {
		MediaServerService.AsyncClient service = createMediaServerServiceAsync();
		mediaServerServicesAsyncInUse.add(service);

		if (mediaServerServicesAsyncInUse.size() > WARN_SIZE)
			log.warn("Numer of serverService clients over warning size");

		return service;
	}

	public void releaseMediaServerServiceAsync(
			MediaServerService.AsyncClient service) {
		mediaServerServicesAsyncInUse.remove(service);
	}

	private MediaServerService.AsyncClient createMediaServerServiceAsync()
			throws TTransportException, IOException {
		TNonblockingTransport transport = new TNonblockingSocket(address, port);
		TAsyncClientManager clientManager = new TAsyncClientManager();
		TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
		return new MediaServerService.AsyncClient(protocolFactory,
				clientManager, transport);
	}

}
