package com.kurento.tool.rom.test.model.client;

import com.kurento.tool.rom.server.Param;

public abstract class MediaServerFactory {

	public abstract SampleClass.Builder createSampleClass(
			@Param("att1") String att1, @Param("att2") boolean att2);
}
