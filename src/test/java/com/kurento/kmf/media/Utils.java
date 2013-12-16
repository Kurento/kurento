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

import java.util.Random;
import java.util.UUID;

import com.kurento.kmf.media.events.MediaError;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.internal.refs.MediaMixerRef;
import com.kurento.kmf.media.internal.refs.MediaObjectRef;
import com.kurento.kmf.media.internal.refs.MediaPadRef;
import com.kurento.kmf.media.internal.refs.MediaPipelineRef;
import com.kurento.kms.thrift.api.KmsMediaElement;
import com.kurento.kms.thrift.api.KmsMediaError;
import com.kurento.kms.thrift.api.KmsMediaEvent;
import com.kurento.kms.thrift.api.KmsMediaMixer;
import com.kurento.kms.thrift.api.KmsMediaObjectRef;
import com.kurento.kms.thrift.api.KmsMediaObjectType;
import com.kurento.kms.thrift.api.KmsMediaPad;
import com.kurento.kms.thrift.api.KmsMediaPadDirection;
import com.kurento.kms.thrift.api.KmsMediaParam;
import com.kurento.kms.thrift.api.KmsMediaPipeline;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
public class Utils {

	private static final Random rnd = new Random(System.nanoTime());

	/**
	 * Creates a {@link KmsMediaEvent}
	 * 
	 * @param ref
	 *            The {@link MediaObjectRef} to the object
	 * 
	 * @param type
	 *            type of the event
	 * @param dataType
	 *            type of the data associated to the event
	 * @param payload
	 *            the serialised data of the event
	 * @return The event
	 */
	public static KmsMediaEvent createKmsEvent(MediaObjectRef ref, String type,
			String dataType, byte[] payload) {
		KmsMediaObjectRef element = ref.getThriftRef();
		return createKmsEvent(element, type, dataType, payload);
	}

	/**
	 * Creates a {@link KmsMediaEvent}
	 * 
	 * @param type
	 *            type of the event
	 * @param dataType
	 *            type of the data associated to the event
	 * @param payload
	 *            the serialised data of the event
	 * @return The event
	 */
	public static KmsMediaEvent createKmsEvent(String type, String dataType,
			byte[] payload) {
		KmsMediaObjectRef element = createKmsElementRef("Any type");
		return createKmsEvent(element, type, dataType, payload);
	}

	private static KmsMediaEvent createKmsEvent(KmsMediaObjectRef ref,
			String type, String dataType, byte[] payload) {
		KmsMediaObjectRef element = ref;
		KmsMediaEvent kmsEvent = new KmsMediaEvent(type, element);
		kmsEvent.setEventData(createKmsParam(dataType, payload));
		return kmsEvent;
	}

	/**
	 * Creates a {@link KmsMediaParam}
	 * 
	 * @param dataType
	 *            Type of data
	 * @param payload
	 *            Serialized data
	 * @return The param
	 */
	public static KmsMediaParam createKmsParam(String dataType, byte[] payload) {
		KmsMediaParam eventData = new KmsMediaParam();
		eventData.dataType = dataType;
		eventData.setData(payload);
		return eventData;
	}

	public static MediaElementRef createMediaElementRef(String typeName) {
		return new MediaElementRef(createKmsElementRef(typeName));
	}

	private static KmsMediaObjectRef createKmsElementRef(String typeName) {
		KmsMediaElement el = new KmsMediaElement(typeName);
		KmsMediaObjectType type = new KmsMediaObjectType();
		type.setElement(el);
		KmsMediaObjectRef objRef = new KmsMediaObjectRef(rnd.nextLong(), UUID
				.randomUUID().toString(), type);
		return objRef;
	}

	public static MediaPipelineRef createMediaPipelineRef() {
		return new MediaPipelineRef(createKmsPipelineRef());
	}

	private static KmsMediaObjectRef createKmsPipelineRef() {
		KmsMediaPipeline pipeline = new KmsMediaPipeline();
		KmsMediaObjectType type = new KmsMediaObjectType();
		type.setPipeline(pipeline);
		KmsMediaObjectRef objRef = new KmsMediaObjectRef(rnd.nextLong(), UUID
				.randomUUID().toString(), type);
		return objRef;
	}

	public static MediaMixerRef createMediaMixerRef(String typeName) {
		return new MediaMixerRef(createKmsMixerRef(typeName));
	}

	private static KmsMediaObjectRef createKmsMixerRef(String typeName) {
		KmsMediaMixer mixer = new KmsMediaMixer(typeName);
		KmsMediaObjectType type = new KmsMediaObjectType();
		type.setMixer(mixer);
		KmsMediaObjectRef objRef = new KmsMediaObjectRef(rnd.nextLong(), UUID
				.randomUUID().toString(), type);
		return objRef;
	}

	public static MediaPadRef createMediaPadRef(MediaType padType,
			KmsMediaPadDirection direction, String description) {
		return new MediaPadRef(createKmsPadRef(padType, direction, description));
	}

	private static KmsMediaObjectRef createKmsPadRef(MediaType padType,
			KmsMediaPadDirection direction, String description) {
		KmsMediaPad pad = new KmsMediaPad(direction, padType.asKmsType(),
				description);
		KmsMediaObjectType type = new KmsMediaObjectType();
		type.setPad(pad);
		KmsMediaObjectRef objRef = new KmsMediaObjectRef(rnd.nextLong(), UUID
				.randomUUID().toString(), type);
		return objRef;
	}

	/**
	 * Creates a {@link MediaError} to be used in tests
	 * 
	 * @param ref
	 *            The reference structure of the object
	 * 
	 * @return The media error
	 */
	public static MediaError createMediaError(MediaObjectRef ref) {
		KmsMediaError kmsError = new KmsMediaError();
		kmsError.description = "An error";
		kmsError.errorCode = rnd.nextInt();
		kmsError.source = ref.getThriftRef();
		kmsError.type = "Some type";
		return (new MediaError(kmsError));
	}

}
