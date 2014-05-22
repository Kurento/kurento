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
package com.kurento.kmf.media.factory;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.tool.rom.client.RemoteObjectFactory;
import com.kurento.tool.rom.client.RemoteObjectTypedFactory;
import com.kurento.tool.rom.transport.jsonrpcconnector.RomClientJsonRpcClient;

/**
 * Factory to create {@link MediaPipeline} in the media server.
 *
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public class MediaPipelineFactory {

	protected RemoteObjectTypedFactory factory;

	public MediaPipelineFactory(JsonRpcClient client) {
		this.factory = new RemoteObjectTypedFactory(new RemoteObjectFactory(
				new RomClientJsonRpcClient(client)));
	}

	/**
	 * Creates a new {@link MediaPipeline} in the media server
	 *
	 * @return The media pipeline
	 */
	public MediaPipeline create() {
		return factory.getFactory(MediaPipeline.Factory.class).create().build();
	}

	/**
	 * Creates a new {@link MediaPipeline} in the media server
	 *
	 * @param cont
	 *            An asynchronous callback handler. If the element was
	 *            successfully created, the {@code onSuccess} method from the
	 *            handler will receive a {@link MediaPipeline} stub from the
	 *            media server.
	 * @throws KurentoMediaFrameworkException
	 *
	 */
	public void create(final Continuation<MediaPipeline> cont)
			throws KurentoMediaFrameworkException {

		factory.getFactory(MediaPipeline.Factory.class).create()
		.buildAsync(cont);
	}

	public void destroy() {
		factory.destroy();
	}

}
