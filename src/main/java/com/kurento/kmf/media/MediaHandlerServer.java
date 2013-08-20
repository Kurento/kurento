package com.kurento.kmf.media;

import java.io.IOException;

import org.apache.thrift.TException;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kms.api.MediaError;
import com.kurento.kms.api.MediaEvent;
import com.kurento.kms.api.MediaHandlerService;

class MediaHandlerServer {

	@Autowired
	private MediaApiConfiguration configuration;

	@Autowired
	private MediaServerHandler handler;

	@Autowired
	private MediaManagerFactory mediaManagerFactory;

	private TServer server;

	MediaHandlerServer() {
	}

	public synchronized void start() throws IOException {
		try {
			TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(
					configuration.getHandlerPort());
			server = new TNonblockingServer(new TNonblockingServer.Args(
					serverTransport).processor(processor));

			new Thread() {
				@Override
				public void run() {
					server.serve(); // TODO manage errors in this thread
				}
			}.start();

		} catch (TTransportException e) {
			throw new IOException(e);
		}
	}

	public synchronized void stop() {
		if (server != null) {
			server.stop();
			server = null;
		}
	}

	private final MediaHandlerService.Processor<MediaHandlerService.Iface> processor = new MediaHandlerService.Processor<MediaHandlerService.Iface>(
			new MediaHandlerService.Iface() {

				@Override
				public void onEvent(MediaEvent event) throws TException {
					// TODO: build a KMF MediaEvent from this KMS MediaEvent and
					// call onEvent over the handler
					MediaObject source = mediaManagerFactory
							.getMediaObject(event.source);
					KmsEvent kmsEvent = source.deserializeEvent(event);
					handler.onEvent(kmsEvent);
				}

				@Override
				public void onError(MediaError error) throws TException {
					// TODO: build a KMF MediaError from this KMS MediaError and
					// call onEvent over the handler
				}
			});

}
