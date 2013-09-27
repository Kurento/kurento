package com.kurento.kmf.media.internal.pool;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kms.thrift.api.MediaServerService.Client;

class MediaServerSyncClientFactory extends BasePoolableObjectFactory<Client> {

	@Autowired
	private MediaApiConfiguration apiConfig;

	@Override
	public Client makeObject() throws KurentoMediaFrameworkException {
		return createSyncClient();
	}

	/**
	 * Validates a {@link Client} before returning it to the queue. This check
	 * is done based on the status of the {@link TTransport} associated with the
	 * client.
	 * 
	 * @param obj
	 *            The object to validate.
	 * @return <code>true</code> if the transport is open.
	 */
	@Override
	public boolean validateObject(Client obj) {
		return obj.getOutputProtocol().getTransport().isOpen();
	}

	/**
	 * Closes the transport
	 * 
	 * @param obj
	 *            The object to destroy.
	 */
	@Override
	public void destroyObject(Client obj) {
		obj.getOutputProtocol().getTransport().close();
	}

	private Client createSyncClient() throws KurentoMediaFrameworkException {
		TSocket socket = new TSocket(this.apiConfig.getServerAddress(),
				this.apiConfig.getServerPort());
		TTransport transport = new TFramedTransport(socket);
		// TODO: Make protocol configurable
		TProtocol prot = new TBinaryProtocol(transport);
		try {
			transport.open();
		} catch (TTransportException e) {
			throw new KurentoMediaFrameworkException(
					"Could not open transport for client", e, 30000);
		}

		return new Client(prot);
	}
}
