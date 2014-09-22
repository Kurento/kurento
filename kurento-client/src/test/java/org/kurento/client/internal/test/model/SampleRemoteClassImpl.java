package org.kurento.client.internal.test.model;

import java.util.Arrays;
import java.util.List;

import org.kurento.client.internal.RemoteClass;
import org.kurento.client.internal.server.Param;

@RemoteClass
public class SampleRemoteClassImpl {

	public void methodReturnVoid() {
	}

	public String methodReturnsString() {
		return "XXXX";
	}

	public boolean methodReturnsBoolean() {
		return false;
	}

	public float methodReturnsFloat() {
		return 0.5f;
	}

	public int methodReturnsInt() {
		return 0;
	}

	public List<String> methodReturnsStringList() {
		return Arrays.asList("XXXX");
	}

	public List<Boolean> methodReturnsBooleanList() {
		return Arrays.asList(false);
	}

	public List<Float> methodReturnsFloatList() {
		return Arrays.asList(0.5f);
	}

	public List<Integer> methodReturnsIntList() {
		return Arrays.asList(0);
	}

	public String methodParamString(@Param("param") String param) {
		return param;
	}

	public boolean methodParamBoolean(@Param("param") boolean param) {
		return param;
	}

	public float methodParamFloat(@Param("param") float param) {
		return param;
	}

	public int methodParamInt(@Param("param") int param) {
		return param;
	}

}
