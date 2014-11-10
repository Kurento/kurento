package org.kurento.tree.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;
import org.kurento.client.KurentoClient;
import org.kurento.commons.PropertiesManager;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.kurento.tree.server.kms.Element;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.WebRtc;
import org.kurento.tree.server.kms.real.RealKms;

public class RealKmsTest {

	@Test
	public void basicTreeTest() throws IOException {

		KurentoServicesTestHelper.startKurentoServicesIfNeccessary();

		RealKms kms = new RealKms(KurentoClient.create(PropertiesManager
				.getProperty(KurentoServicesTestHelper.KMS_WS_URI_PROP,
						KurentoServicesTestHelper.KMS_WS_URI_DEFAULT)));

		Pipeline pipeline = kms.createPipeline();
		WebRtc master = pipeline.createWebRtc();

		for (int i = 0; i < 3; i++) {
			WebRtc viewer = pipeline.createWebRtc();
			master.connect(viewer);
		}

		assertThat(master.getSinks().size(), is(3));

		for (Element sink : master.getSinks()) {
			assertThat(master, is(sink.getSource()));
		}

		for (Element sink : new ArrayList<>(master.getSinks())) {
			sink.disconnect();
			assertThat(sink.getSource(), is(nullValue()));
		}

		assertThat(master.getSinks(), is(Collections.<Element> emptyList()));

		KurentoServicesTestHelper.teardownServices();
	}
}
