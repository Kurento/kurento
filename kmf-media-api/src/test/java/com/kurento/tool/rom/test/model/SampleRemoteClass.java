package com.kurento.tool.rom.test.model;

import java.util.List;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

@RemoteClass
public interface SampleRemoteClass {

	public void methodReturnVoid();

	public String methodReturnsString();

	public boolean methodReturnsBoolean();

	public float methodReturnsFloat();

	public int methodReturnsInt();

	public List<String> methodReturnsStringList();

	public List<Boolean> methodReturnsBooleanList();

	public List<Float> methodReturnsFloatList();

	public List<Integer> methodReturnsIntList();

	public String methodParamString(@Param("param") String param);

	public boolean methodParamBoolean(@Param("param") boolean param);

	public float methodParamFloat(@Param("param") float param);

	public int methodParamInt(@Param("param") int param);

}
