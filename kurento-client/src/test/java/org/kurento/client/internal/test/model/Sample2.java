package org.kurento.client.internal.test.model;

import org.kurento.client.AbstractBuilder;
import org.kurento.client.KurentoObject;
import org.kurento.client.internal.client.RomManager;

public interface Sample2 extends KurentoObject {

	public String getAtt1();

	public int getAtt2();

	public float getAtt3();

	public boolean getAtt4();

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
	}
}
