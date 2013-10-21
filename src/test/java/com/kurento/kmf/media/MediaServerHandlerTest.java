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

import static com.kurento.kmf.media.Utils.createKmsEvent;
import static com.kurento.kmf.media.Utils.createMediaError;
import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.VOID_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaPlayerEndPointTypeConstants.EVENT_EOS;

import java.util.UUID;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaError;
import com.kurento.kmf.media.events.MediaErrorListener;
import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.internal.ErrorListenerRegistration;
import com.kurento.kmf.media.internal.EventListenerRegistration;
import com.kurento.kmf.media.internal.MediaServerCallbackHandler;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kms.thrift.api.KmsMediaEvent;

/**
 * Test suite for the handling of errors and events from the KMS
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/kmf-api-test-context.xml")
public class MediaServerHandlerTest {

	private static final String EVENT_TEST_TYPE = "Fake event type";

	private String callbackToken;

	private final MediaElementRef ref = Utils
			.createMediaElementRef(EVENT_TEST_TYPE);

	private MediaElement element;

	private MediaEvent propagatedEvent;

	@Autowired
	private ApplicationContext ctx;

	@Autowired
	private MediaServerCallbackHandler handler;

	@Before
	public void setup() {
		element = (MediaElement) ctx.getBean("mediaObject", ref);
		callbackToken = UUID.randomUUID().toString();
	}

	@Test
	public void testPropagateError() {
		MediaErrorListener errorListener = new MediaErrorListener() {
			@Override
			public void onError(MediaError error) {
				Assert.assertEquals(error.getObjectRef(), ref);
			}
		};

		ErrorListenerRegistration registration = new ErrorListenerRegistration(
				callbackToken);

		handler.addErrorListener(element, registration, errorListener);

		MediaError error = createMediaError(ref);

		handler.onError(registration, ref.getId(), error);
	}

	@Test
	public void testPropagateEosEvent() {

		MediaEventListener<EndOfStreamEvent> eosListener = new MediaEventListener<EndOfStreamEvent>() {

			@Override
			public void onEvent(EndOfStreamEvent eosEvent) {
				propagatedEvent = eosEvent;
			}

		};

		EventListenerRegistration registration = new EventListenerRegistration(
				callbackToken);
		handler.addListener(element, registration, eosListener);

		KmsMediaEvent kmsEvent = createKmsEvent(ref, EVENT_EOS, VOID_DATA_TYPE,
				null);

		EndOfStreamEvent event = (EndOfStreamEvent) ctx.getBean("mediaEvent",
				kmsEvent);

		handler.onEvent(registration, ref.getId(), event);

		Assert.assertEquals(propagatedEvent.getSource(), element);
	}

}
