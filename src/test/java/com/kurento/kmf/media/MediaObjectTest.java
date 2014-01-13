/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.media;

import static com.kurento.kmf.media.Utils.createMediaElementRef;
import static com.kurento.kmf.media.Utils.createMediaMixerRef;
import static com.kurento.kmf.media.Utils.createMediaPadRef;
import static com.kurento.kmf.media.Utils.createMediaPipelineRef;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kmf.media.internal.ChromaFilterImpl;
import com.kurento.kmf.media.internal.FaceOverlayFilterImpl;
import com.kurento.kmf.media.internal.GStreamerFilterImpl;
import com.kurento.kmf.media.internal.HttpEndpointImpl;
import com.kurento.kmf.media.internal.JackVaderFilterImpl;
import com.kurento.kmf.media.internal.MainMixerImpl;
import com.kurento.kmf.media.internal.MediaElementImpl;
import com.kurento.kmf.media.internal.MediaMixerImpl;
import com.kurento.kmf.media.internal.MediaPipelineImpl;
import com.kurento.kmf.media.internal.MediaSinkImpl;
import com.kurento.kmf.media.internal.MediaSourceImpl;
import com.kurento.kmf.media.internal.PlateDetectorFilterImpl;
import com.kurento.kmf.media.internal.PlayerEndpointImpl;
import com.kurento.kmf.media.internal.RecorderEndpointImpl;
import com.kurento.kmf.media.internal.RtpEndpointImpl;
import com.kurento.kmf.media.internal.WebRtcEndpointImpl;
import com.kurento.kmf.media.internal.ZBarFilterImpl;
import com.kurento.kmf.media.internal.refs.MediaObjectRef;
import com.kurento.kms.thrift.api.KmsMediaChromaFilterTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaFaceOverlayFilterTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaGStreamerFilterTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaHttpEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaJackVaderFilterTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaPadDirection;
import com.kurento.kms.thrift.api.KmsMediaPlateDetectorFilterTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaPlayerEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaRecorderEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaRtpEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaWebRtcEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaZBarFilterTypeConstants;

/**
 * This test class checks the correct creation of MediaObjects
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/kmf-api-test-context.xml")
public class MediaObjectTest {

	private static final String JACK_VADER_FILTER_TYPE = KmsMediaJackVaderFilterTypeConstants.TYPE_NAME;
	private static final String ZBAR_FILTER_TYPE = KmsMediaZBarFilterTypeConstants.TYPE_NAME;
	private static final String HTTP_EP_TYPE = KmsMediaHttpEndPointTypeConstants.TYPE_NAME;
	private static final String PLAYER_EP_TYPE = KmsMediaPlayerEndPointTypeConstants.TYPE_NAME;
	private static final String RECORDER_EP_TYPE = KmsMediaRecorderEndPointTypeConstants.TYPE_NAME;
	private static final String WEB_RTC_EP_TYPE = KmsMediaWebRtcEndPointTypeConstants.TYPE_NAME;
	private static final String RTP_EP_TYPE = KmsMediaRtpEndPointTypeConstants.TYPE_NAME;
	private static final String PLATE_DETECTOR_FILTER_TYPE = KmsMediaPlateDetectorFilterTypeConstants.TYPE_NAME;
	private static final String FACE_OVERLAY_TYPE = KmsMediaFaceOverlayFilterTypeConstants.TYPE_NAME;
	private static final String CHROMA_FILTER_TYPE = KmsMediaChromaFilterTypeConstants.TYPE_NAME;
	private static final String GSTREAMER_FILTER_TYPE = KmsMediaGStreamerFilterTypeConstants.TYPE_NAME;

	@Autowired
	private ApplicationContext ctx;

	@Test
	public void testMediaElementInstantiation() {
		MediaObjectRef objRef = createMediaElementRef("AnyNonThriftName");
		instantiateAndCheck(MediaElementImpl.class, objRef);
	}

	@Test
	public void testMediaSinkInstantiation() {
		MediaObjectRef objRef = createMediaPadRef(MediaType.AUDIO,
				KmsMediaPadDirection.SINK, "media sink");
		instantiateAndCheck(MediaSinkImpl.class, objRef);
	}

	@Test
	public void testMediaSourceInstantiation() {
		MediaObjectRef objRef = createMediaPadRef(MediaType.AUDIO,
				KmsMediaPadDirection.SRC, "media source");
		instantiateAndCheck(MediaSourceImpl.class, objRef);
	}

	@Test
	public void testMediaPipelineInstantiation() {
		MediaObjectRef objRef = createMediaPipelineRef();
		instantiateAndCheck(MediaPipelineImpl.class, objRef);
	}

	@Test
	public void testJackVaderFilterInstantiation() {
		MediaObjectRef objRef = createMediaElementRef(JACK_VADER_FILTER_TYPE);
		instantiateAndCheck(JackVaderFilterImpl.class, objRef);
	}

	@Test
	public void testZBarFilterInstantiation() {
		MediaObjectRef objRef = createMediaElementRef(ZBAR_FILTER_TYPE);
		instantiateAndCheck(ZBarFilterImpl.class, objRef);
	}

	@Test
	public void testHttpEndpointInstantiation() {
		MediaObjectRef objRef = createMediaElementRef(HTTP_EP_TYPE);
		instantiateAndCheck(HttpEndpointImpl.class, objRef);
	}

	@Test
	public void testWebRtcEndpointInstantiation() {
		MediaObjectRef objRef = createMediaElementRef(WEB_RTC_EP_TYPE);
		instantiateAndCheck(WebRtcEndpointImpl.class, objRef);
	}

	@Test
	public void testPlayerEndpointInstantiation() {
		MediaObjectRef objRef = createMediaElementRef(PLAYER_EP_TYPE);
		instantiateAndCheck(PlayerEndpointImpl.class, objRef);
	}

	@Test
	public void testRtpEndpointInstantiation() {
		MediaObjectRef objRef = createMediaElementRef(RTP_EP_TYPE);
		instantiateAndCheck(RtpEndpointImpl.class, objRef);

	}

	@Test
	public void testRecorderEndpointInstantiation() {
		MediaObjectRef objRef = createMediaElementRef(RECORDER_EP_TYPE);
		instantiateAndCheck(RecorderEndpointImpl.class, objRef);
	}

	@Test
	public void testFaceOverlayFilterInstantiation() {
		MediaObjectRef objRef = createMediaElementRef(FACE_OVERLAY_TYPE);
		instantiateAndCheck(FaceOverlayFilterImpl.class, objRef);
	}

	@Test
	public void testPlateDetectorFilterInstantiation() {
		MediaObjectRef objRef = createMediaElementRef(PLATE_DETECTOR_FILTER_TYPE);
		instantiateAndCheck(PlateDetectorFilterImpl.class, objRef);
	}

	@Test
	public void testChromaFilterInstantiation() {
		MediaObjectRef objRef = createMediaElementRef(CHROMA_FILTER_TYPE);
		instantiateAndCheck(ChromaFilterImpl.class, objRef);
	}

	@Test
	public void testGStreamerFilterInstantiation() {
		MediaObjectRef objRef = createMediaElementRef(GSTREAMER_FILTER_TYPE);
		instantiateAndCheck(GStreamerFilterImpl.class, objRef);
	}

	@Test
	public void testMainMixerInstantiation() {
		MediaObjectRef objRef = createMediaMixerRef(MainMixerImpl.TYPE);
		instantiateAndCheck(MainMixerImpl.class, objRef);
	}

	@Test
	public void testMediaMixerInstantiation() {
		MediaObjectRef objRef = createMediaMixerRef(MediaMixerImpl.TYPE);
		instantiateAndCheck(MediaMixerImpl.class, objRef);
	}

	private void instantiateAndCheck(Class<?> expectedClass,
			MediaObjectRef objRef) {
		final MediaObject obj = (MediaObject) ctx
				.getBean("mediaObject", objRef);
		Assert.assertEquals(obj.getClass(), expectedClass);
	}

}
