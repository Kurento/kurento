package org.kurento.client.test.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.internal.server.KurentoServerException;
import org.kurento.client.test.util.MediaPipelineBaseTest;

public class SdpEndpointApiTest extends MediaPipelineBaseTest {

	private WebRtcEndpoint webrtcEP;

	@Test
	public void generateOfferTest() {

		assertThat(webrtcEP.generateOffer(), not(isEmptyOrNullString()));

	}

	public void getLocalSessionDescriptorTest() {

		assertThat(webrtcEP.getLocalSessionDescriptor(),
				not(isEmptyOrNullString()));

	}

	@Test(expected = KurentoServerException.class)
	public void getLocalSessionDescriptorWithFailTest() {

		assertThat(webrtcEP.getLocalSessionDescriptor(), isEmptyOrNullString());

	}

	@Test
	public void getRemoteSessionDescriptorTest() {

		assertThat(webrtcEP.getRemoteSessionDescriptor(),
				not(isEmptyOrNullString()));

	}

	@Test(expected = KurentoServerException.class)
	public void getRemoteSessionDescriptorWithFailTest() {

		webrtcEP.getRemoteSessionDescriptor();

	}

	@Test
	public void getSetMaxVideoRecvBandwidthTest() {

		webrtcEP.setMaxVideoRecvBandwidth(500);
		assertThat(webrtcEP.getMaxVideoRecvBandwidth(), is(500));
		webrtcEP.setMaxVideoRecvBandwidth(1500);
		assertThat(webrtcEP.getMaxVideoRecvBandwidth(), is(1500));

	}

	@Test
	public void processAnswerTest() {

		webrtcEP.processAnswer("Mi olla descontrola");

	}

	@Test(expected = KurentoServerException.class)
	public void processAnswerWithFailTest() {
		// TODO Create a valid SDP answer. This one shouldn't work!
		webrtcEP.processAnswer("Mi olla descontrola");

	}

	@Test
	public void processOfferTest() {
		// TODO Create a valid SDP offer. This one shouldn't work!
		String sdpAnswer = webrtcEP.processOffer("Mi olla descontrola");
		assertThat(sdpAnswer, not(isEmptyOrNullString()));

	}

	@Test(expected = KurentoServerException.class)
	public void processOfferWithFailTest() {

		webrtcEP.processOffer("Mi olla descontrola");

	}

	@Before
	public void setupMediaElements() {
		webrtcEP = new WebRtcEndpoint.Builder(pipeline).build();
	}

	@After
	public void teardownMediaElements() {
		webrtcEP.release();
	}

}
