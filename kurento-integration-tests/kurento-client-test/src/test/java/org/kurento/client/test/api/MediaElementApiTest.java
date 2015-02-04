package org.kurento.client.test.api;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.AudioCaps;
import org.kurento.client.AudioCodec;
import org.kurento.client.Fraction;
import org.kurento.client.MediaElement;
import org.kurento.client.MediaType;
import org.kurento.client.VideoCaps;
import org.kurento.client.VideoCodec;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.test.util.MediaPipelineBaseTest;

public class MediaElementApiTest extends MediaPipelineBaseTest {

	private WebRtcEndpoint webrtcEP;

	@Before
	public void setupMediaElements() {
		webrtcEP = new WebRtcEndpoint.Builder(pipeline).build();
	}

	@After
	public void teardownMediaElements() {
		webrtcEP.release();
	}

	/**
	 * Test the method {@link MediaElement#connect(MediaElement)}. Asserts that
	 * a media element can be connected in loopback
	 *
	 */
	@Test
	public void connectTest() {

		webrtcEP.connect(webrtcEP);
	}

	/**
	 * Test the method {@link MediaElement#connect(MediaElement, MediaType)}.
	 * Asserts that a media element can be connected in loopback
	 *
	 */
	@Test
	public void connectMediaTypeTest() {

		for (MediaType type : MediaType.values()) {
			webrtcEP.connect(webrtcEP, type);
		}

	}

	/**
	 * Test the method
	 * {@link MediaElement#connect(MediaElement, MediaType, String)}.
	 *
	 */
	@Test
	public void connectMediaTypeAndDescriptionTest() {

		for (MediaType type : MediaType.values()) {
			webrtcEP.connect(webrtcEP, type,
					"Yet another media connect test with media type " + type);
		}

	}

	/**
	 * Test the method {@link MediaElement#setAudioFormat(AudioCaps)}.
	 *
	 */
	@Test
	public void setAudioFormatTest() {

		for (AudioCodec codec : AudioCodec.values()) {
			webrtcEP.setAudioFormat(new AudioCaps(codec, 100));
		}

	}

	/**
	 * Test the method {@link MediaElement#setVideoFormatTest(VideoCaps)}.
	 *
	 */
	@Test
	public void setVideoFormatTest() {

		for (VideoCodec codec : VideoCodec.values()) {
			webrtcEP.setVideoFormat(new VideoCaps(codec, new Fraction(500, 400)));
		}

	}
}
