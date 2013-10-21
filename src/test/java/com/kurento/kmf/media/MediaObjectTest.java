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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kmf.media.internal.HttpEndPointImpl;
import com.kurento.kmf.media.internal.JackVaderFilterImpl;
import com.kurento.kmf.media.internal.MainMixerImpl;
import com.kurento.kmf.media.internal.MediaElementImpl;
import com.kurento.kmf.media.internal.MediaMixerImpl;
import com.kurento.kmf.media.internal.MediaPipelineImpl;
import com.kurento.kmf.media.internal.MediaSinkImpl;
import com.kurento.kmf.media.internal.MediaSourceImpl;
import com.kurento.kmf.media.internal.PlayerEndPointImpl;
import com.kurento.kmf.media.internal.RecorderEndPointImpl;
import com.kurento.kmf.media.internal.RtpEndPointImpl;
import com.kurento.kmf.media.internal.WebRtcEndPointImpl;
import com.kurento.kmf.media.internal.ZBarFilterImpl;
import com.kurento.kmf.media.internal.refs.MediaObjectRef;
import com.kurento.kms.thrift.api.KmsMediaHttpEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaJackVaderFilterTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaPadDirection;
import com.kurento.kms.thrift.api.KmsMediaPlayerEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaRecorderEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaRtpEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaType;
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

	@Autowired
	private ApplicationContext ctx;

	@Test
	public void testMediaElementInstantiation() {
		MediaObjectRef objRef = createMediaElementRef("AnyNonThriftName");
		instantiateAndCheck(MediaElementImpl.class, objRef);
	}

	@Test
	public void testMediaSinkInstantiation() {
		MediaObjectRef objRef = createMediaPadRef(KmsMediaType.AUDIO,
				KmsMediaPadDirection.SINK, "media sink");
		instantiateAndCheck(MediaSinkImpl.class, objRef);
	}

	@Test
	public void testMediaSourceInstantiation() {
		MediaObjectRef objRef = createMediaPadRef(KmsMediaType.AUDIO,
				KmsMediaPadDirection.SRC, "media source");
		instantiateAndCheck(MediaSourceImpl.class, objRef);
	}

	// TODO activate after correcting error found in the instantiation of
	// MediaMixers
	@Ignore
	@Test
	public void testMediaMixerInstantiation() {
		MediaObjectRef objRef = createMediaMixerRef("BaseFilter");
		instantiateAndCheck(MediaMixerImpl.class, objRef);
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
	public void testHttpEndPointInstantiation() {
		MediaObjectRef objRef = createMediaElementRef(HTTP_EP_TYPE);
		instantiateAndCheck(HttpEndPointImpl.class, objRef);
	}

	@Test
	public void testWebRtcEndpointInstantiation() {
		MediaObjectRef objRef = createMediaElementRef(WEB_RTC_EP_TYPE);
		instantiateAndCheck(WebRtcEndPointImpl.class, objRef);
	}

	@Test
	public void testPlayerEndPointInstantiation() {
		MediaObjectRef objRef = createMediaElementRef(PLAYER_EP_TYPE);
		instantiateAndCheck(PlayerEndPointImpl.class, objRef);
	}

	@Test
	public void testRtpEndPointInstantiation() {
		MediaObjectRef objRef = createMediaElementRef(RTP_EP_TYPE);
		instantiateAndCheck(RtpEndPointImpl.class, objRef);

	}

	@Test
	public void testRecorderEndPointInstantiation() {
		MediaObjectRef objRef = createMediaElementRef(RECORDER_EP_TYPE);
		instantiateAndCheck(RecorderEndPointImpl.class, objRef);
	}

	// TODO activate after correcting error found in the instantiation of
	// MediaMixers
	@Ignore
	@Test
	public void testMainMixerInstantiation() {
		MediaObjectRef objRef = createMediaMixerRef(MainMixerImpl.TYPE);
		instantiateAndCheck(MediaMixerImpl.class, objRef);
	}

	private void instantiateAndCheck(Class<?> expectedClass,
			MediaObjectRef objRef) {
		final MediaObject obj = (MediaObject) ctx
				.getBean("mediaObject", objRef);
		Assert.assertEquals(obj.getClass(), expectedClass);
	}

}
