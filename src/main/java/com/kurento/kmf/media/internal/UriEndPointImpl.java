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
import com.kurento.kmf.media.UriEndPoint;
import com.kurento.kmf.media.commands.internal.GetUriCommand;
import com.kurento.kmf.media.commands.internal.PauseCommand;
import com.kurento.kmf.media.commands.internal.StartCommand;
import com.kurento.kmf.media.commands.internal.StopCommand;
import com.kurento.kmf.media.commands.internal.StringCommandResult;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

public abstract class UriEndPointImpl extends EndPointImpl implements
		UriEndPoint {

	UriEndPointImpl(MediaElementRefDTO endpointRef) {
		super(endpointRef);
	}

	/* SYNC */
	@Override
	public String getUri() {
		StringCommandResult result = (StringCommandResult) sendCommand(new GetUriCommand());
		return result.getResult();
	}

	void start() {
		sendCommand(new StartCommand());
	}

	@Override
	public void pause() {
		sendCommand(new PauseCommand());
	}

	@Override
	public void stop() {
		sendCommand(new StopCommand());
	}

	/* ASYNC */
	@Override
	public void getUri(final Continuation<String> cont) {
		sendCommand(new GetUriCommand(), new StringContinuationWrapper(cont));
	}

	void start(final Continuation<Void> cont) {
		sendCommand(new StartCommand(), new VoidContinuationWrapper(cont));
	}

	@Override
	public void pause(final Continuation<Void> cont) {
		sendCommand(new PauseCommand(), new VoidContinuationWrapper(cont));
	}

	@Override
	public void stop(final Continuation<Void> cont) {
		sendCommand(new StopCommand(), new VoidContinuationWrapper(cont));
	}
}
