package org.kurento.client.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ FaceOverlayFilterTest.class, GStreamerFilterTest.class,
		HttpGetEndpointTest.class, PlayerEndpointTest.class,
		RecorderEndpointTest.class, RtpEndpoint2Test.class,
		RtpEndpointTest.class, WebRtcEndpointTest.class, ZBarFilterTest.class })
public class ApiTests {

}
