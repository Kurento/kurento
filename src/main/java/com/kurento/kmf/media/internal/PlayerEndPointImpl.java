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

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.PlayerEndPoint;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kms.thrift.api.mediaServerConstants;

@ProvidesMediaElement(type = mediaServerConstants.PLAYER_END_POINT_TYPE)
public class PlayerEndPointImpl extends UriEndPointImpl implements
		PlayerEndPoint {

	PlayerEndPointImpl(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	@Override
	public void play() {
		start();
	}

	/* ASYNC */
	@Override
	public void play(final Continuation<Void> cont) {
		start(cont);
	}
}
