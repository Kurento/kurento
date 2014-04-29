package com.kurento.tool.rom.test.model;

import java.util.Arrays;
import java.util.List;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

@RemoteClass
public class SampleRemoteClassImpl implements SampleRemoteClass {

	@Override
	public void methodReturnVoid() {
	}

	@Override
	public String methodReturnsString() {
		return "XXXX";
	}

	@Override
	public boolean methodReturnsBoolean() {
		return false;
	}

	@Override
	public float methodReturnsFloat() {
		return 0.5f;
	}

	@Override
	public int methodReturnsInt() {
		return 0;
	}

	@Override
	public List<String> methodReturnsStringList() {
		return Arrays.asList("XXXX");
	}

	@Override
	public List<Boolean> methodReturnsBooleanList() {
		return Arrays.asList(false);
	}

	@Override
	public List<Float> methodReturnsFloatList() {
		return Arrays.asList(0.5f);
	}

	@Override
	public List<Integer> methodReturnsIntList() {
		return Arrays.asList(0);
	}

	@Override
	public String methodParamString(@Param("param") String param) {
		return param;
	}

	@Override
	public boolean methodParamBoolean(@Param("param") boolean param) {
		return param;
	}

	@Override
	public float methodParamFloat(@Param("param") float param) {
		return param;
	}

	@Override
	public int methodParamInt(@Param("param") int param) {
		return param;
	}

}
