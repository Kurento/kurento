package org.kurento.tool.rom.test.model.client;

import org.kurento.tool.rom.server.Param;

public class ComplexParam {

	private String prop1;
	private int prop2;
	private String prop3;
	private float prop4;

	public ComplexParam(@Param("prop1") String prop1, @Param("prop2") int prop2) {
		this.prop1 = prop1;
		this.prop2 = prop2;
	}

	public String getProp1() {
		return prop1;
	}

	public void setProp1(String prop1) {
		this.prop1 = prop1;
	}

	public int getProp2() {
		return prop2;
	}

	public void setProp2(int prop2) {
		this.prop2 = prop2;
	}

	public String getProp3() {
		return prop3;
	}

	public void setProp3(String prop3) {
		this.prop3 = prop3;
	}

	public float getProp4() {
		return prop4;
	}

	public void setProp4(float prop4) {
		this.prop4 = prop4;
	}

}
