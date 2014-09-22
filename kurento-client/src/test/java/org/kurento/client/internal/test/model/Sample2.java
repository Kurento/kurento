package org.kurento.client.internal.test.model;

import org.kurento.client.AbstractBuilder;
import org.kurento.client.KurentoObject;
import org.kurento.client.Transaction;
import org.kurento.client.internal.client.RemoteObjectFacade;
import org.kurento.client.internal.client.RomManager;

public class Sample2 extends KurentoObject {

	public Sample2(RemoteObjectFacade remoteObject) {
		super(remoteObject, null);
	}

	public String getAtt1() {
		return (String) remoteObject.invoke("getAtt1", null, String.class);
	}

	public int getAtt2() {
		return (int) remoteObject.invoke("getAtt2", null, int.class);
	}

	public float getAtt3() {
		return (float) remoteObject.invoke("getAtt3", null, float.class);
	}

	public boolean getAtt4() {
		return (boolean) remoteObject.invoke("getAtt4", null, boolean.class);
	}

	public static Builder with(String att1, int att2, RomManager manager) {
		return new Builder(att1, att2, manager);
	}

	public static class Builder extends AbstractBuilder<Sample2> {

		public Builder(String att1, int att2, RomManager manager) {

			super(Sample2.class, manager);
			props.add("att1", att1);
			props.add("att2", att2);
		}

		public Builder withAtt3(float att3) {
			props.add("att3", att3);
			return this;
		}

		public Builder att4() {
			props.add("att4", Boolean.TRUE);
			return this;
		}

		@Override
		protected Sample2 createMediaObject(RemoteObjectFacade remoteObject,
				Transaction tx) {
			return new Sample2(remoteObject);
		}
	}
}
