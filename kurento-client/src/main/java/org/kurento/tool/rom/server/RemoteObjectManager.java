package org.kurento.tool.rom.server;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.kurento.common.SecretGenerator;
import org.kurento.tool.rom.transport.serialization.ObjectRefsManager;

public class RemoteObjectManager implements ObjectRefsManager {

	// This class is used to control equals behavior of values in the biMap
	// regardless equals in remote classes
	public static class ObjectHolder {
		private Object object;

		public ObjectHolder(Object object) {
			this.object = object;
		}

		public Object getObject() {
			return object;
		}

		@Override
		public int hashCode() {
			return object.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			ObjectHolder other = (ObjectHolder) obj;
			return object == other.object;
		}
	}

	private SecretGenerator secretGenerator = new SecretGenerator();
	private BiMap<String, ObjectHolder> remoteObjects = HashBiMap.create();

	public String putObject(Object object) {
		String nextSecret;
		do {
			nextSecret = secretGenerator.nextSecret();
		} while (remoteObjects.get(nextSecret) != null);

		remoteObjects.put(nextSecret, new ObjectHolder(object));

		return nextSecret;
	}

	@Override
	public Object getObject(String objectRef) {
		return remoteObjects.get(objectRef).getObject();
	}

	public void releaseObject(String objectRef) {
		this.remoteObjects.remove(objectRef);
	}

	public String getObjectRefFrom(Object object) {
		return remoteObjects.inverse().get(new ObjectHolder(object));
	}

}
