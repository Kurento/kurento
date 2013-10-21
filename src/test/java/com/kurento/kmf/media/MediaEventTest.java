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

import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.VOID_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaPlayerEndPointTypeConstants.EVENT_EOS;
import static com.kurento.kms.thrift.api.KmsMediaSessionEndPointTypeConstants.EVENT_MEDIA_SESSION_COMPLETE;
import static com.kurento.kms.thrift.api.KmsMediaSessionEndPointTypeConstants.EVENT_MEDIA_SESSION_START;
import static com.kurento.kms.thrift.api.KmsMediaZBarFilterTypeConstants.EVENT_CODE_FOUND;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kmf.media.events.internal.CodeFoundEventImpl;
import com.kurento.kmf.media.events.internal.DefaultMediaEventImpl;
import com.kurento.kmf.media.events.internal.EndOfStreamEventImpl;
import com.kurento.kmf.media.events.internal.MediaSessionStartedEventImpl;
import com.kurento.kmf.media.events.internal.MediaSessionTerminatedEventImpl;
import com.kurento.kms.thrift.api.KmsMediaElement;
import com.kurento.kms.thrift.api.KmsMediaEvent;
import com.kurento.kms.thrift.api.KmsMediaObjectRef;
import com.kurento.kms.thrift.api.KmsMediaObjectType;
import com.kurento.kms.thrift.api.KmsMediaParam;

//TODO create new events to check deserialization, although there are separate tests for media params
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/kmf-api-test-context.xml")
public class MediaEventTest {

	@Autowired
	private ApplicationContext ctx;

	@Test
	public void testCodeFoundEventInstantiation() {
		KmsMediaEvent kmsEvent = createKmsEvent(EVENT_CODE_FOUND,
				VOID_DATA_TYPE);
		instantiateAndCheck(CodeFoundEventImpl.class, kmsEvent);
	}

	@Test
	public void testDefaultMediaEventInstantiation() {
		KmsMediaEvent kmsEvent = createKmsEvent("DefaultEvent", VOID_DATA_TYPE);
		instantiateAndCheck(DefaultMediaEventImpl.class, kmsEvent);
	}

	@Test
	public void testEndOfStreamEventInstantiation() {
		KmsMediaEvent kmsEvent = createKmsEvent(EVENT_EOS, VOID_DATA_TYPE);
		instantiateAndCheck(EndOfStreamEventImpl.class, kmsEvent);
	}

	@Test
	public void testMediaSessionStartedEventInstantiation() {
		KmsMediaEvent kmsEvent = createKmsEvent(EVENT_MEDIA_SESSION_START,
				VOID_DATA_TYPE);
		instantiateAndCheck(MediaSessionStartedEventImpl.class, kmsEvent);
	}

	@Test
	public void testMediaSessionTerminatedEventInstantiation() {
		KmsMediaEvent kmsEvent = createKmsEvent(EVENT_MEDIA_SESSION_COMPLETE,
				VOID_DATA_TYPE);
		instantiateAndCheck(MediaSessionTerminatedEventImpl.class, kmsEvent);
	}

	private KmsMediaEvent createKmsEvent(String type, String dataType) {
		KmsMediaParam eventData = new KmsMediaParam();
		eventData.dataType = dataType;
		KmsMediaElement element = new KmsMediaElement("Some type");
		KmsMediaObjectType objType = new KmsMediaObjectType();
		objType.setElement(element);
		KmsMediaObjectRef objRef = new KmsMediaObjectRef(1, "", objType);
		KmsMediaEvent kmsEvent = new KmsMediaEvent(type, objRef);
		kmsEvent.setEventData(eventData);
		return kmsEvent;
	}

	private MediaEvent instantiateAndCheck(Class<?> expectedClass,
			KmsMediaEvent kmsEvent) {
		final MediaEvent event = (MediaEvent) ctx.getBean("mediaEvent",
				kmsEvent);
		Assert.assertEquals(event.getClass(), expectedClass);
		return event;
	}
}
