package org.kurento.tree.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;
import org.kurento.client.factory.KurentoClient;
import org.kurento.tree.server.kms.Element;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.WebRtc;
import org.kurento.tree.server.kms.real.RealKms;

public class RealKmsTest {

	@Test
	public void basicTreeTest() {

		RealKms kms = new RealKms(
				KurentoClient.create("ws://localhost:8888/kurento"));
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

	}

}
