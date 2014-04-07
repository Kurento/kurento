package com.kurento.tool.rom.test.model.server;

import java.util.List;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.EventManager;
import com.kurento.tool.rom.server.InjectEventManager;
import com.kurento.tool.rom.server.Param;
import com.kurento.tool.rom.test.model.client.ComplexParam;
import com.kurento.tool.rom.test.model.client.SampleEnum;

@RemoteClass
public class SampleClassImpl {

	@InjectEventManager
	private EventManager eventManager;

	private final String att1;
	private final boolean att2;
	private final float att3;
	private final int att4;

	public SampleClassImpl(@Param("att1") String att1,
			@Param("att2") boolean att2, @Param("att3") float att3,
			@Param("att4") int att4) {
		this.att1 = att1;
		this.att2 = att2;
		this.att3 = att3;
		this.att4 = att4;
	}

	public String getAtt1() {
		return att1;
	}

	public boolean getAtt2() {
		return att2;
	}

	public float getAtt3() {
		return att3;
	}

	public int getAtt4() {
		return att4;
	}

	public SampleEnum echoEnum(@Param("param") SampleEnum param) {
		return param;
	}

	public ComplexParam echoRegister(@Param("param") ComplexParam param) {
		return param;
	}

	public List<SampleEnum> echoListEnum(@Param("param") List<SampleEnum> param) {
		return param;
	}

	public List<ComplexParam> echoListRegister(
			@Param("param") List<ComplexParam> param) {
		return param;
	}

	public SampleClassImpl echoObjectRef(@Param("param") SampleClassImpl param) {
		return param;
	}

	public List<SampleClassImpl> echoObjectRefList(
			@Param("param") List<SampleClassImpl> param) {
		return param;
	}

	// public void startTestEvents(@Param("numEvents") final int numEvents) {
	//
	// new Thread() {
	// public void run() {
	// for(int i = 0; i<numEvents; i++) {
	// try {
	// Thread.sleep(1000);
	// } catch (InterruptedException e) {}
	// try {
	// eventManager.fireEvent(new SampleEvent("prop1", "prop2"));
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	// }
	// }.start();
	// }

}
