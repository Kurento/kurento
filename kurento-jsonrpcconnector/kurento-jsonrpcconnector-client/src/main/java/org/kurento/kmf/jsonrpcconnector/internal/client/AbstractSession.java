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
package org.kurento.kmf.jsonrpcconnector.internal.client;

import org.kurento.kmf.jsonrpcconnector.Session;

public abstract class AbstractSession implements Session {

	private String sessionId;
	private Object registerInfo;
	private boolean newSession = true;

	public AbstractSession(String sessionId, Object registerInfo) {
		this.sessionId = sessionId;
		this.registerInfo = registerInfo;
	}

	@Override
	public Object getRegisterInfo() {
		return registerInfo;
	}

	public void setRegisterInfo(Object registerInfo) {
		this.registerInfo = registerInfo;
	}

	@Override
	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public boolean isNew() {
		return newSession;
	}

	public void setNew(boolean newSession) {
		this.newSession = newSession;
	}

}
