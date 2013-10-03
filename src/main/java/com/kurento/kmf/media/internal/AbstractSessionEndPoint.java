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

import static com.kurento.kms.thrift.api.KmsMediaSessionEndPointTypeConstants.EVENT_MEDIA_SESSION_COMPLETE;
import static com.kurento.kms.thrift.api.KmsMediaSessionEndPointTypeConstants.EVENT_MEDIA_SESSION_START;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.ListenerRegistration;
import com.kurento.kmf.media.SessionEndPoint;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.MediaSessionStartedEvent;
import com.kurento.kmf.media.events.MediaSessionTerminatedEvent;
import com.kurento.kmf.media.internal.refs.MediaElementRef;

public abstract class AbstractSessionEndPoint extends AbstractEndPoint
		implements SessionEndPoint {

	public AbstractSessionEndPoint(MediaElementRef endpointRef) {
		super(endpointRef);
	}

	/* SYNC */

	@Override
	public ListenerRegistration addMediaSessionCompleteListener(
			final MediaEventListener<MediaSessionTerminatedEvent> sessionEvent) {
		return addListener(EVENT_MEDIA_SESSION_COMPLETE, sessionEvent);
	}

	@Override
	public ListenerRegistration addMediaSessionStartListener(
			final MediaEventListener<MediaSessionStartedEvent> sessionEvent) {
		return addListener(EVENT_MEDIA_SESSION_START, sessionEvent);
	}

	/* ASYNC */

	@Override
	public void addMediaSessionCompleteListener(
			final MediaEventListener<MediaSessionTerminatedEvent> sessionEvent,
			final Continuation<ListenerRegistration> cont) {
		addListener(EVENT_MEDIA_SESSION_COMPLETE, sessionEvent, cont);
	}

	@Override
	public void addMediaSessionStartListener(
			final MediaEventListener<MediaSessionStartedEvent> sessionEvent,
			final Continuation<ListenerRegistration> cont) {
		addListener(EVENT_MEDIA_SESSION_START, sessionEvent, cont);
	}

}
