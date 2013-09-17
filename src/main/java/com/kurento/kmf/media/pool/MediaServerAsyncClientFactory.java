package com.kurento.kmf.media.pool;

import java.io.IOException;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.thrift.async.TAsyncClient;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TNonblockingTransport;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.internal.MediaApiConfiguration;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient;

class MediaServerAsyncClientFactory extends
		BasePoolableObjectFactory<AsyncClient> {

	@Autowired
	private MediaApiConfiguration apiConfig;

	@Override
	public AsyncClient makeObject() throws KurentoMediaFrameworkException {
		return createAsyncClient();
	}

	/**
	 * Validates a {@link Client} before returning it to the queue. This check
	 * is done based on {@link TAsyncClient#hasError()}.
	 * 
	 * @param obj
	 *            The object to validate.
	 * @return <code>true</code> If the client has no error
	 */
	@Override
	public boolean validateObject(AsyncClient obj) {
		// TODO check if this is enough to guarantee the client
		return !obj.hasError();
	}

	@Override
	public void destroyObject(AsyncClient obj) {
		// TODO add impl if needed
	}

	private AsyncClient createAsyncClient()
			throws KurentoMediaFrameworkException {
		TNonblockingTransport transport;

		try {
			transport = new TNonblockingSocket(apiConfig.getServerAddress(),
					apiConfig.getServerPort());
		} catch (IOException e) {
			throw new KurentoMediaFrameworkException(
					"Error creating non blocking transport", e, 30000);
		}

		TAsyncClientManager clientManager;
		try {
			clientManager = new TAsyncClientManager();
		} catch (IOException e) {
			throw new KurentoMediaFrameworkException(
					"Error creating client manager", e, 30000);
		}

		TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();

		return new AsyncClient(protocolFactory, clientManager, transport);
	}

}
