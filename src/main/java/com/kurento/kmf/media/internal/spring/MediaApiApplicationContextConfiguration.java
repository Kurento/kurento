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
package com.kurento.kmf.media.internal.spring;

import java.lang.reflect.Constructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.media.MediaMixer;
import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.commands.MediaCommandResult;
import com.kurento.kmf.media.commands.internal.AbstractMediaCommandResult;
import com.kurento.kmf.media.events.MediaError;
import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kmf.media.events.internal.AbstractMediaEvent;
import com.kurento.kmf.media.events.internal.DefaultMediaEventImpl;
import com.kurento.kmf.media.internal.MediaElementImpl;
import com.kurento.kmf.media.internal.MediaHandlerServer;
import com.kurento.kmf.media.internal.MediaPipelineImpl;
import com.kurento.kmf.media.internal.MediaServerCallbackHandler;
import com.kurento.kmf.media.internal.MediaSinkImpl;
import com.kurento.kmf.media.internal.MediaSourceImpl;
import com.kurento.kmf.media.internal.ProvidesMediaCommand;
import com.kurento.kmf.media.internal.pool.MediaServerClientPoolService;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kmf.media.internal.refs.MediaMixerRefDTO;
import com.kurento.kmf.media.internal.refs.MediaObjectRefDTO;
import com.kurento.kmf.media.internal.refs.MediaPadRefDTO;
import com.kurento.kmf.media.internal.refs.MediaPipelineRefDTO;
import com.kurento.kms.thrift.api.Command;
import com.kurento.kms.thrift.api.KmsError;
import com.kurento.kms.thrift.api.KmsEvent;
import com.kurento.kms.thrift.api.PadDirection;
import com.kurento.kms.thrift.api.Params;

@Configuration
public class MediaApiApplicationContextConfiguration {

	@Bean
	MediaServerClientPoolService mediaServerClientPoolService() {
		return new MediaServerClientPoolService();
	}

	@Bean
	MediaHandlerServer mediaHandlerServer() {
		return new MediaHandlerServer();
	}

	@Bean
	MediaPipelineFactory mediaPipelineFactory() {
		return new MediaPipelineFactory();
	}

	@Bean
	MediaApiConfiguration mediaApiConfiguration() {
		return new MediaApiConfiguration();
	}

	@Bean
	MediaServerCallbackHandler mediaServerCallbackHandler() {
		return new MediaServerCallbackHandler();
	}

	@Bean
	MediaElementClassStore mediaElementClassStore() {
		return new MediaElementClassStore();
	}

	@Bean
	MediaEventClassStore mediaEventClassStore() {
		return new MediaEventClassStore();
	}

	@Bean
	@Scope("prototype")
	public MediaEvent mediaEvent(KmsEvent event) {

		Class<?> clazz = mediaEventClassStore().get(event.getType());

		if (clazz == null) {
			clazz = DefaultMediaEventImpl.class;
		}

		AbstractMediaEvent mediaEvent;
		try {
			// TODO: Document that all event classes must have one constructor
			// taking a KmsEvent
			Constructor<?> constructor = clazz.getConstructor(KmsEvent.class);
			// This cast is safe as long as the type of the class refers to a
			// type that extends from AbstarctMediaEvent.
			// Nevertheless, a catch is included in the try-catch block.
			mediaEvent = (AbstractMediaEvent) constructor.newInstance(event);
		} catch (Exception e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

		if (event.isSetEventData()) {
			mediaEvent.deserializeData(event);
		}

		return mediaEvent;
	}

	@Bean
	@Scope("prototype")
	public MediaError mediaError(KmsError error) {
		return new MediaError(error);
	}

	@Bean
	@Scope("prototype")
	public MediaCommandResult mediaCommandResult(Command command, Params result) {

		ProvidesMediaCommand annotation = command.getClass().getAnnotation(
				ProvidesMediaCommand.class);
		Class<?> clazz = annotation.resultClass();

		AbstractMediaCommandResult mediaCommandResult;

		try {
			// TODO: document that command result must have a default
			// constructor
			Constructor<?> constructor = clazz.getConstructor();
			// This cast is safe as long as the type of the class refers to a
			// type that extends from MediaObject.
			// Nevertheless, a catch is included in the try-catch block.
			mediaCommandResult = (AbstractMediaCommandResult) constructor
					.newInstance();
		} catch (Exception e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

		if (result.isSetData()) {
			mediaCommandResult.deserializeCommandResult(result);
		}

		return mediaCommandResult;
	}

	@Bean
	@Scope("prototype")
	public MediaObject mediaObject(MediaObjectRefDTO objRef) {

		Class<?> clazz;
		if (objRef instanceof MediaPadRefDTO) {
			MediaPadRefDTO padRefDTO = (MediaPadRefDTO) objRef;
			clazz = classFromPadDirection(padRefDTO.getPadDirection());

		} else if (objRef instanceof MediaElementRefDTO) {
			MediaElementRefDTO elementRefDTO = (MediaElementRefDTO) objRef;
			String type = elementRefDTO.getType();
			clazz = mediaEventClassStore().get(type);
			if (clazz == null) {
				clazz = MediaElementImpl.class;
			}

		} else if (objRef instanceof MediaMixerRefDTO) {
			MediaMixerRefDTO mixerRefDTO = (MediaMixerRefDTO) objRef;
			String type = mixerRefDTO.getType();
			clazz = mediaEventClassStore().get(type);

			if (clazz == null) {
				clazz = MediaMixer.class;
			}

		} else if (objRef instanceof MediaPipelineRefDTO) {
			clazz = MediaPipelineImpl.class;

		} else {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(
					"objRef is not of any known type", 30000);
		}

		MediaObject obj = instantiateMediaObject(clazz, objRef);

		return obj;
	}

	private static Class<?> classFromPadDirection(PadDirection padDirection) {
		Class<?> clazz;
		switch (padDirection) {
		case SINK:
			clazz = MediaSinkImpl.class;
			break;
		case SRC:
			clazz = MediaSourceImpl.class;
			break;
		default:
			// TODO error code
			throw new KurentoMediaFrameworkException(
					"Invalid MediaPad direction", 30000);
		}
		return clazz;
	}

	private static MediaObject instantiateMediaObject(Class<?> clazz,
			MediaObjectRefDTO objRef) {

		// TODO Change error code
		Assert.notNull(clazz, "MediaObject class not found", 30000);

		MediaObject obj;
		try {
			// TODO: document that all media objects must have such constructor
			Constructor<?> constructor = clazz
					.getConstructor(MediaObjectRefDTO.class);
			// This cast is safe as long as the type of the class refers to a
			// type that extends from MediaObject.
			// Nevertheless, a catch is included in the try-catch block.
			obj = (MediaObject) constructor.newInstance(objRef);
		} catch (Exception e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

		return obj;
	}

}
