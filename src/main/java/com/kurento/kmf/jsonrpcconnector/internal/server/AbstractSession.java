package com.kurento.kmf.jsonrpcconnector.internal.server;

import com.kurento.kmf.jsonrpcconnector.Session;

public abstract class AbstractSession implements Session {

	private String sessionId;
	private Object registerInfo;
	private boolean newSession = true;

	public AbstractSession(String sessionId, Object registerInfo) {
		this.sessionId = sessionId;
		this.registerInfo = registerInfo;
	}

	public Object getRegisterInfo() {
		return registerInfo;
	}

	public void setRegisterInfo(Object registerInfo) {
		this.registerInfo = registerInfo;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public boolean isNew() {
		return newSession;
	}

	public void setNew(boolean newSession) {
		this.newSession = newSession;
	}

}
