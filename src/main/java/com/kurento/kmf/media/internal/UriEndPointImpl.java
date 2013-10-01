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

import static com.kurento.kms.thrift.api.UriEndPointTypeConstants.GET_URI;
import static com.kurento.kms.thrift.api.UriEndPointTypeConstants.PAUSE;
import static com.kurento.kms.thrift.api.UriEndPointTypeConstants.START;
import static com.kurento.kms.thrift.api.UriEndPointTypeConstants.STOP;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.UriEndPoint;
import com.kurento.kmf.media.commands.internal.StringCommandResult;
import com.kurento.kmf.media.commands.internal.VoidCommand;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

public abstract class UriEndPointImpl extends EndPointImpl implements
		UriEndPoint {

	UriEndPointImpl(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	@Override
	public String getUri() {
		StringCommandResult result = (StringCommandResult) sendCommand(new VoidCommand(
				GET_URI));
		return result.getResult();
	}

	void start() {
		sendCommand(new VoidCommand(START));
	}

	@Override
	public void pause() {
		sendCommand(new VoidCommand(PAUSE));
	}

	@Override
	public void stop() {
		sendCommand(new VoidCommand(STOP));
	}

	/* ASYNC */
	@Override
	public void getUri(final Continuation<String> cont) {
		sendCommand(new VoidCommand(GET_URI), new StringContinuationWrapper(
				cont));
	}

	void start(final Continuation<Void> cont) {
		sendCommand(new VoidCommand(START), new VoidContinuationWrapper(cont));
	}

	@Override
	public void pause(final Continuation<Void> cont) {
		sendCommand(new VoidCommand(PAUSE), new VoidContinuationWrapper(cont));
	}

	@Override
	public void stop(final Continuation<Void> cont) {
		sendCommand(new VoidCommand(STOP), new VoidContinuationWrapper(cont));
	}
}
