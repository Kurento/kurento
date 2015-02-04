package org.kurento.client.test.api;

import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.MediaPipeline;
import org.kurento.client.test.util.MediaPipelineBaseTest;
import org.kurento.module.plumberendpoint.PlumberEndpoint;

public class PlumberEndpointApiTest extends MediaPipelineBaseTest {

	private PlumberEndpoint plumber;
	private MediaPipeline receiverPipeline;

	@Test
	public void linkAndGetAddressAndPortTest() {
		PlumberEndpoint receiver = new PlumberEndpoint.Builder(receiverPipeline)
				.build();

		plumber.link(receiver.getAddress(), receiver.getPort());
		receiver.release();
	}

	@Test
	public void linkInvalidAddressTest() {
		assertFalse(plumber.link("wrong address", 8080));
	}

	@Test
	public void linkInvalidPootTest() {
		assertFalse(plumber.link("192.168.0.1", 0));
		assertFalse(plumber.link("192.168.0.1", -1234));
		assertFalse(plumber.link("192.168.0.1", 87240));
	}

	@Before
	public void setupMediaElements() {
		plumber = new PlumberEndpoint.Builder(pipeline).build();
		receiverPipeline = kurentoClient.createMediaPipeline();
	}

	@After
	public void teardownMediaElements() {
		plumber.release();

		if (receiverPipeline != null) {
			receiverPipeline.release();
		}
	}

}
