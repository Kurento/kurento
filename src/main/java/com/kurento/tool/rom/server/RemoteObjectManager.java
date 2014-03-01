package com.kurento.tool.rom.server;

import java.util.HashMap;
import java.util.Map;

import com.kurento.kmf.common.SecretGenerator;

public class RemoteObjectManager {

	private SecretGenerator secretGenerator = new SecretGenerator();
	private Map<String, Object> remoteObjects = new HashMap<String, Object>();

	public String putObject(Object object) {
		String nextSecret;
		do {
			nextSecret = secretGenerator.nextSecret();
		} while (remoteObjects.get(nextSecret) != null);

		remoteObjects.put(nextSecret, object);

		return nextSecret;
	}

	public Object getObject(String objectRef) {
		return remoteObjects.get(objectRef);
	}

	public void releaseObject(String objectRef) {
		this.remoteObjects.remove(objectRef);
	}

}
