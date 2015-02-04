package org.kurento.client.test.api;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.HttpEndpoint;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.test.util.MediaPipelineBaseTest;

public class HttpEndpointApiTest extends MediaPipelineBaseTest {

	private HttpGetEndpoint httpEp;

	@Before
	public void setupMediaElements() {
		httpEp = new HttpGetEndpoint.Builder(pipeline).build();
	}

	@After
	public void teardownMediaElements() {
		httpEp.release();
	}

	/**
	 * Checks that {@link HttpEndpoint#getUrl()} method does not return an empty
	 * string
	 *
	 */
	@Test
	public void testMethodGetUrl() {

		assertThat(httpEp.getUrl(), not(isEmptyOrNullString()));

	}
}
