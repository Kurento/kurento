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
package org.kurento.jsonrpc.internal.client;

import org.kurento.jsonrpc.Session;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (newSession ? 1231 : 1237);
		result = prime * result
				+ ((registerInfo == null) ? 0 : registerInfo.hashCode());
		result = prime * result
				+ ((sessionId == null) ? 0 : sessionId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractSession other = (AbstractSession) obj;
		if (newSession != other.newSession)
			return false;
		if (registerInfo == null) {
			if (other.registerInfo != null)
				return false;
		} else if (!registerInfo.equals(other.registerInfo))
			return false;
		if (sessionId == null) {
			if (other.sessionId != null)
				return false;
		} else if (!sessionId.equals(other.sessionId))
			return false;
		return true;
	}

}
