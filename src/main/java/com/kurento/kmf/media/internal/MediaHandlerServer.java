package com.kurento.kmf.media.internal;

import java.io.IOException;

import org.apache.thrift.TException;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;

import com.kurento.kmf.media.MediaManagerHandler;
import com.kurento.kms.api.MediaError;
import com.kurento.kms.api.MediaEvent;
import com.kurento.kms.api.MediaHandlerService;

class MediaHandlerServer {

	private final int port;
	private final MediaManagerHandler handler;
	private TServer server;

	public MediaHandlerServer(int port, MediaManagerHandler handler) {
		this.port = port;
		this.handler = handler;
	}

	public synchronized void start() throws IOException {
		try {
			TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(
					port);
			server = new TNonblockingServer(new TNonblockingServer.Args(
					serverTransport).processor(processor));

			new Thread() {
				@Override
				public void run() {
					server.serve();
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
				public void onEvent(MediaEvent arg0) throws TException {
					// TODO: build a KMF MediaEvent from this KMS MediaEvent and
					// call onEvent over the handler
				}

				@Override
				public void onError(MediaError arg0) throws TException {
					// TODO: build a KMF MediaError from this KMS MediaError and
					// call onEvent over the handler
				}
			});

}
