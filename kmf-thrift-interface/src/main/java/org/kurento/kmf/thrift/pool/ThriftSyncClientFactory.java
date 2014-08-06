/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.kmf.thrift.pool;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.kurento.kmf.common.exception.KurentoException;
import org.kurento.kmf.thrift.ThriftInterfaceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

public class ThriftSyncClientFactory extends BasePooledObjectFactory<Client> {

	@Autowired
	private ThriftInterfaceConfiguration apiConfig;

	/**
	 * Default constructor, to be used in spring environments
	 */
	public ThriftSyncClientFactory() {
	}

	/**
	 * Constructor for non-spring environments.
	 * 
	 * @param apiConfig
	 *            configuration object
	 */
	public ThriftSyncClientFactory(ThriftInterfaceConfiguration apiConfig) {
		this.apiConfig = apiConfig;
	}

	@Override
	public Client create() throws KurentoException {
		return createSyncClient();
	}

	@Override
	public PooledObject<Client> wrap(Client obj) {
		return new DefaultPooledObject<>(obj);
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
	public boolean validateObject(PooledObject<Client> obj) {
		return ((ClientWithValidation) obj.getObject()).isValid();
	}

	/**
	 * Closes the transport
	 * 
	 * @param obj
	 *            The object to destroy.
	 */
	@Override
	public void destroyObject(PooledObject<Client> obj) {
		obj.getObject().getOutputProtocol().getTransport().close();
		obj.getObject().getInputProtocol().getTransport().close();
	}

	private ClientWithValidation createSyncClient() {
		TSocket socket = new TSocket(this.apiConfig.getServerAddress(),
				this.apiConfig.getServerPort());
		TTransport transport = new TFramedTransport(socket);
		// TODO: Make protocol configurable
		TProtocol prot = new TBinaryProtocol(transport);
		try {
			transport.open();
		} catch (TTransportException e) {
			throw new ClientPoolException(
					"Could not open transport for sync client with "
							+ this.apiConfig.getServerAddress() + ":"
							+ this.apiConfig.getServerPort(), e);
		}

		return new ClientWithValidation(prot);
	}

}
