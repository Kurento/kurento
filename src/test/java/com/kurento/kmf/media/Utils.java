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

import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.internal.refs.MediaMixerRef;
import com.kurento.kmf.media.internal.refs.MediaPadRef;
import com.kurento.kmf.media.internal.refs.MediaPipelineRef;
import com.kurento.kms.thrift.api.KmsMediaElement;
import com.kurento.kms.thrift.api.KmsMediaEvent;
import com.kurento.kms.thrift.api.KmsMediaMixer;
import com.kurento.kms.thrift.api.KmsMediaObjectRef;
import com.kurento.kms.thrift.api.KmsMediaObjectType;
import com.kurento.kms.thrift.api.KmsMediaPad;
import com.kurento.kms.thrift.api.KmsMediaPadDirection;
import com.kurento.kms.thrift.api.KmsMediaParam;
import com.kurento.kms.thrift.api.KmsMediaPipeline;
import com.kurento.kms.thrift.api.KmsMediaType;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
public class Utils {

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
		KmsMediaObjectRef objRef = new KmsMediaObjectRef(1, "token", type);
		return objRef;
	}

	public static MediaPipelineRef createMediaPipelineRef() {
		return new MediaPipelineRef(createKmsPipelineRef());
	}

	private static KmsMediaObjectRef createKmsPipelineRef() {
		KmsMediaPipeline pipeline = new KmsMediaPipeline();
		KmsMediaObjectType type = new KmsMediaObjectType();
		type.setPipeline(pipeline);
		KmsMediaObjectRef objRef = new KmsMediaObjectRef(1, "", type);
		return objRef;
	}

	public static MediaMixerRef createMediaMixerRef(String typeName) {
		return new MediaMixerRef(createKmsMixerRef(typeName));
	}

	private static KmsMediaObjectRef createKmsMixerRef(String typeName) {
		KmsMediaMixer mixer = new KmsMediaMixer(typeName);
		KmsMediaObjectType type = new KmsMediaObjectType();
		type.setMixer(mixer);
		KmsMediaObjectRef objRef = new KmsMediaObjectRef(1, "token", type);
		return objRef;
	}

	public static MediaPadRef createMediaPadRef(KmsMediaType padType,
			KmsMediaPadDirection direction, String description) {
		return new MediaPadRef(createKmsPadRef(padType, direction, description));
	}

	private static KmsMediaObjectRef createKmsPadRef(KmsMediaType padType,
			KmsMediaPadDirection direction, String description) {
		KmsMediaPad pad = new KmsMediaPad(direction, padType, description);
		KmsMediaObjectType type = new KmsMediaObjectType();
		type.setPad(pad);
		KmsMediaObjectRef objRef = new KmsMediaObjectRef(1, "token", type);
		return objRef;
	}
}
