package org.kurento.tool.rom.test.model;

import org.kurento.tool.rom.RemoteClass;
import org.kurento.tool.rom.server.Param;

@RemoteClass
public class Sample2Impl implements Sample2 {

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

	@Override
	public String getAtt1() {
		return att1;
	}

	@Override
	public int getAtt2() {
		return att2;
	}

	@Override
	public float getAtt3() {
		return att3;
	}

	@Override
	public boolean getAtt4() {
		return att4;
	}

}
