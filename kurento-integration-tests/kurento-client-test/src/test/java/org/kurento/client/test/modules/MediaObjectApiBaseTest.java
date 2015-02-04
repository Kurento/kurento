package org.kurento.client.test.modules;

import org.junit.After;
import org.kurento.client.MediaObject;
import org.kurento.client.test.util.MediaPipelineBaseTest;

public abstract class MediaObjectApiBaseTest<T extends MediaObject> extends
		MediaPipelineBaseTest {

	public abstract T getMediaObject();

	@After
	public void teardownMediaObjects() {

		if (this.getMediaObject() != null) {
			this.getMediaObject().release();
		}
	}

}
