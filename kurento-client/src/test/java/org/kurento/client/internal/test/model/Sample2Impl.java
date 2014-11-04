package org.kurento.client.internal.test.model;

import org.kurento.client.internal.RemoteClass;
import org.kurento.client.internal.server.Param;

@RemoteClass
public class Sample2Impl {

	private String att1;
	private int att2;
	private float att3;
	private boolean att4;

	public Sample2Impl(@Param("att1") String att1, @Param("att2") int att2,
			@Param("att3") float att3, @Param("att4") boolean att4) {
		this.att1 = att1;
		this.att2 = att2;
		this.att3 = att3;
		this.att4 = att4;
	}

	public String getAtt1() {
		return att1;
	}

	public int getAtt2() {
		return att2;
	}

	public float getAtt3() {
		return att3;
	}

	public boolean getAtt4() {
		return att4;
	}
}
