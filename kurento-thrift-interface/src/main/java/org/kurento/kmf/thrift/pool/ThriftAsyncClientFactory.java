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

import java.io.IOException;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.async.TAsyncClient;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TNonblockingTransport;
import org.kurento.kmf.common.exception.KurentoException;
import org.kurento.kmf.thrift.ThriftInterfaceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;

public class ThriftAsyncClientFactory extends
		BasePooledObjectFactory<AsyncClient> {

	@Autowired
	private ThriftInterfaceConfiguration apiConfig;

	private TAsyncClientManager clientManager;

	/**
	 * Default constructor, to be used in spring environments
	 */
	public ThriftAsyncClientFactory() {

		try {
			clientManager = new TAsyncClientManager();
		} catch (IOException e) {
			throw new ClientPoolException("Error creating client manager", e);
		}
	}

	/**
	 * Constructor for non-spring environments.
	 * 
	 * @param apiConfig
	 *            configuration object
	 */
	public ThriftAsyncClientFactory(ThriftInterfaceConfiguration apiConfig) {
		this();
		this.apiConfig = apiConfig;
	}

	@Override
	public AsyncClient create() throws KurentoException {
		return createAsyncClient();
	}

	@Override
	public PooledObject<AsyncClient> wrap(AsyncClient obj) {
		return new DefaultPooledObject<>(obj);
	}

	/**
	 * Validates an {@link AsyncClient} before returning it to the queue. This
	 * check is done based on {@link TAsyncClient#hasError()}.
	 * 
	 * @param obj
	 *            The object to validate.
	 * @return <code>true</code> If the client has no error
	 */
	@Override
	public boolean validateObject(PooledObject<AsyncClient> obj) {
		return ((AsyncClientWithValidation) obj.getObject()).isValid();
	}

	@Override
	public void destroyObject(PooledObject<AsyncClient> obj) {
		// TODO close the transport if needed
	}

	private AsyncClient createAsyncClient() {
		TNonblockingTransport transport;

		try {
			transport = new TNonblockingSocket(apiConfig.getServerAddress(),
					apiConfig.getServerPort());
		} catch (IOException e) {
			throw new ClientPoolException(
					"Error creating non blocking transport for asynchronous client with \"\n"
							+ this.apiConfig.getServerAddress() + ":"
							+ this.apiConfig.getServerPort(), e);
		}

		TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();

		return new AsyncClientWithValidation(protocolFactory, clientManager,
				transport);
	}

}
