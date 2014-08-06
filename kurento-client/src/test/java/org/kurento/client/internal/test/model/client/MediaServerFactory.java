package org.kurento.client.internal.test.model.client;

import org.kurento.client.internal.server.Param;

public abstract class MediaServerFactory {

	public abstract SampleClass.Builder createSampleClass(
			@Param("att1") String att1, @Param("att2") boolean att2);
}
