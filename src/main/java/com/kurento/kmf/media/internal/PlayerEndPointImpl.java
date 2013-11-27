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
package com.kurento.kmf.media.internal;

import static com.kurento.kms.thrift.api.KmsMediaPlayerEndPointTypeConstants.EVENT_EOS;
import static com.kurento.kms.thrift.api.KmsMediaPlayerEndPointTypeConstants.TYPE_NAME;

import java.net.URI;
import java.util.Map;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.ListenerRegistration;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndPoint;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.params.MediaParam;

@ProvidesMediaElement(type = TYPE_NAME)
public class PlayerEndPointImpl extends AbstractUriEndPoint implements
		PlayerEndPoint {

	public PlayerEndPointImpl(MediaElementRef endpointRef) {
		super(endpointRef);
	}

	/**
	 * @param objectRef
	 * @param params
	 */
	public PlayerEndPointImpl(MediaElementRef objectRef,
			Map<String, MediaParam> params) {
		super(objectRef, params);
	}

	@Override
	public void play() {
		start();
	}

	@Override
	public ListenerRegistration addEndOfStreamListener(
			final MediaEventListener<EndOfStreamEvent> eosEvent) {
		return addListener(EVENT_EOS, eosEvent);
	}

	/* ASYNC */

	@Override
	public void play(final Continuation<Void> cont) {
		start(cont);
	}

	@Override
	public void addEndOfStreamListener(
			final MediaEventListener<EndOfStreamEvent> eosEvent,
			final Continuation<ListenerRegistration> cont) {
		addListener(EVENT_EOS, eosEvent, cont);
	}

	public static class PlayerEndPointBuilderImpl<T extends PlayerEndPointBuilderImpl<T>>
			extends AbstractUriEndPointBuilder<T, PlayerEndPoint> implements
			PlayerEndPointBuilder {

		public PlayerEndPointBuilderImpl(final URI uri,
				final MediaPipeline pipeline) {
			super(uri, TYPE_NAME, pipeline);
		}

	}

}
