/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TNonblockingTransport;

import com.kurento.kms.thrift.api.KmsMediaServerService;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;

/**
 * Thrift client that adds a validation method. This class is to be used
 * internally, and for external users of the pool it should be transparent. The
 * validating clients have been implemented to overcome the impossibility, found
 * during several trials, of correctly asserting whether the client is still
 * valid.
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 *
 */
class AsyncClientWithValidation extends AsyncClient {

	/**
	 * @param protocolFactory
	 * @param clientManager
	 * @param transport
	 */
	public AsyncClientWithValidation(TProtocolFactory protocolFactory,
			TAsyncClientManager clientManager, TNonblockingTransport transport) {
		super(protocolFactory, clientManager, transport);
	}

	@Override
	public void invokeJsonRpc(String request,
			final AsyncMethodCallback<invokeJsonRpc_call> resultHandler)
			throws TException {

		// FIXME: Implement retries
		super.invokeJsonRpc(
				request,
				new AsyncMethodCallback<KmsMediaServerService.AsyncClient.invokeJsonRpc_call>() {

					@Override
					public void onComplete(
							KmsMediaServerService.AsyncClient.invokeJsonRpc_call response) {
						resultHandler.onComplete(response);
					}

					@Override
					public void onError(Exception exception) {
						resultHandler.onError(exception);
					}
				});
	}

	public boolean isValid() {
		boolean isValid = true;
		try {
			this.checkReady();
		} catch (IllegalStateException e) {
			isValid = false;
		}
		return isValid;
	}

}
