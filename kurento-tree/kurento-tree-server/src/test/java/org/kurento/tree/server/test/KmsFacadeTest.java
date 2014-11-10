package org.kurento.tree.server.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;
import org.kurento.tree.server.kms.Element;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.WebRtc;

public class KmsFacadeTest {

	@Test
	public void basicTreeTest() {

		Kms kms = new Kms();
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
