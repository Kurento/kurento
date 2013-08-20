package com.kurento.kmf.media;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

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
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;

class MediaServerServiceManager {

	private static Logger log = LoggerFactory
			.getLogger(MediaServerServiceManager.class);

	private static final int WARN_SIZE = 3;

	@Autowired
	private MediaHandlerServer mediaHandlerServer;

	@Autowired
	private MediaApiConfiguration configuration;

	@Autowired
	private MediaServerHandler handler;

	// TODO: question, what's the objective of this?
	private final Set<MediaServerService.Client> mediaServerServicesInUse = new CopyOnWriteArraySet<MediaServerService.Client>();
	private final Set<MediaServerService.AsyncClient> mediaServerServicesAsyncInUse = new CopyOnWriteArraySet<MediaServerService.AsyncClient>();

	MediaServerServiceManager() {
	}

	@PostConstruct
	public void init() throws IOException {

		mediaHandlerServer.start();

		MediaServerService.Client service = getMediaServerService();
		try {
			service.addHandlerAddress(handler.getHandlerId(),
					configuration.getHandlerAddress(),
					configuration.getHandlerPort());
		} catch (MediaServerException e) {
			throw new IOException(e);
		} catch (TException e) {
			throw new IOException(e);
		} finally {
			releaseMediaServerService(service);
		}
	}

	@PreDestroy
	public synchronized void destroy() {
		mediaHandlerServer.stop();
	}

	private MediaServerService.Client createMediaServerService()
			throws TTransportException {
		// FIXME: use pool to avoid no such sockets
		TTransport transport = new TFramedTransport(
				new TSocket(configuration.getServerAddress(),
						configuration.getServerPort()));
		// TODO: Make protocol configurable
		TProtocol prot = new TBinaryProtocol(transport);
		transport.open();
		return new MediaServerService.Client(prot);
	}

	public MediaServerService.Client getMediaServerService() throws IOException {

		try {
			MediaServerService.Client service = createMediaServerService();
			mediaServerServicesInUse.add(service);

			if (mediaServerServicesInUse.size() > WARN_SIZE)
				log.warn("Number of serverService clients over warning size");

			return service;
		} catch (TTransportException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public void releaseMediaServerService(MediaServerService.Client service) {

		mediaServerServicesInUse.remove(service);

		service.getInputProtocol().getTransport().close();
		service.getOutputProtocol().getTransport().close();
	}

	private MediaServerService.AsyncClient createMediaServerServiceAsync()
			throws IOException {
		TNonblockingTransport transport = new TNonblockingSocket(
				configuration.getServerAddress(), configuration.getServerPort());
		TAsyncClientManager clientManager = new TAsyncClientManager();
		TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
		return new MediaServerService.AsyncClient(protocolFactory,
				clientManager, transport);
	}

	public MediaServerService.AsyncClient getMediaServerServiceAsync()
			throws IOException {
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
