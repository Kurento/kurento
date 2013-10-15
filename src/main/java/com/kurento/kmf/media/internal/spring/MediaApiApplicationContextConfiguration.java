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
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.media.MediaMixer;
import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.MediaPipelineFactory;
import com.kurento.kmf.media.events.MediaError;
import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kmf.media.events.internal.AbstractMediaEvent;
import com.kurento.kmf.media.events.internal.DefaultMediaEventImpl;
import com.kurento.kmf.media.internal.DistributedGarbageCollector;
import com.kurento.kmf.media.internal.MediaElementImpl;
import com.kurento.kmf.media.internal.MediaHandlerServer;
import com.kurento.kmf.media.internal.MediaPipelineImpl;
import com.kurento.kmf.media.internal.MediaServerCallbackHandler;
import com.kurento.kmf.media.internal.MediaSinkImpl;
import com.kurento.kmf.media.internal.MediaSourceImpl;
import com.kurento.kmf.media.internal.pool.MediaServerAsyncClientFactory;
import com.kurento.kmf.media.internal.pool.MediaServerAsyncClientPool;
import com.kurento.kmf.media.internal.pool.MediaServerClientPoolService;
import com.kurento.kmf.media.internal.pool.MediaServerSyncClientFactory;
import com.kurento.kmf.media.internal.pool.MediaServerSyncClientPool;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.internal.refs.MediaMixerRef;
import com.kurento.kmf.media.internal.refs.MediaObjectRef;
import com.kurento.kmf.media.internal.refs.MediaPadRef;
import com.kurento.kmf.media.internal.refs.MediaPipelineRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.AbstractMediaParam;
import com.kurento.kms.thrift.api.KmsMediaError;
import com.kurento.kms.thrift.api.KmsMediaEvent;
import com.kurento.kms.thrift.api.KmsMediaPadDirection;
import com.kurento.kms.thrift.api.KmsMediaParam;

@Configuration
public class MediaApiApplicationContextConfiguration {

	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(10);
		executor.initialize();
		return executor;
	}

	@Bean
	MediaServerAsyncClientFactory mediaServerAsyncClientFactory() {
		return new MediaServerAsyncClientFactory();
	}

	@Bean
	MediaServerSyncClientFactory mediaServerSyncClientFactory() {
		return new MediaServerSyncClientFactory();
	}

	@Bean
	MediaServerClientPoolService mediaServerClientPoolService() {
		return new MediaServerClientPoolService();
	}

	@Bean
	MediaServerSyncClientPool mediaServerSyncClientPool() {
		return new MediaServerSyncClientPool();
	}

	@Bean
	MediaServerAsyncClientPool mediaServerAsyncClientPool() {
		return new MediaServerAsyncClientPool();
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
	MediaParamClassStore mediaParamClassStore() {
		return new MediaParamClassStore();
	}

	@Bean
	DistributedGarbageCollector distributedGarbageCollector() {
		return new DistributedGarbageCollector();
	}

	@Bean
	@Scope("prototype")
	public MediaEvent mediaEvent(KmsMediaEvent event) {

		Class<?> clazz = mediaEventClassStore().get(event.getType());

		if (clazz == null) {
			clazz = DefaultMediaEventImpl.class;
		}

		AbstractMediaEvent<?> mediaEvent;
		try {
			// TODO: Document that all event classes must have one constructor
			// taking a KmsMediaEvent
			Constructor<?> constructor = clazz
					.getConstructor(KmsMediaEvent.class);
			// This cast is safe as long as the type of the class refers to a
			// type that extends from AbstarctMediaEvent.
			// Nevertheless, a catch is included in the try-catch block.
			mediaEvent = (AbstractMediaEvent<?>) constructor.newInstance(event);
		} catch (Exception e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

		return mediaEvent;
	}

	@Bean
	@Scope("prototype")
	public MediaError mediaError(KmsMediaError error) {
		return new MediaError(error);
	}

	@Bean
	@Scope("prototype")
	public MediaParam mediaParam(KmsMediaParam param) {

		Class<?> clazz = mediaParamClassStore().get(param.dataType);

		AbstractMediaParam mediaParam;

		try {
			// TODO: document that command result must have a default
			// constructor
			Constructor<?> constructor = clazz.getConstructor();
			// This cast is safe as long as the type of the class refers to a
			// type that extends from MediaObject.
			// Nevertheless, a catch is included in the try-catch block.
			mediaParam = (AbstractMediaParam) constructor.newInstance();
		} catch (Exception e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

		if (param.isSetData()) {
			mediaParam.deserializeParam(param);
		}

		return mediaParam;
	}

	@Bean
	@Scope("prototype")
	public MediaObject mediaObject(MediaObjectRef objRef) {

		Class<?> clazz;
		if (objRef instanceof MediaPadRef) {
			MediaPadRef padRefDTO = (MediaPadRef) objRef;
			clazz = classFromPadDirection(padRefDTO.getPadDirection());

		} else if (objRef instanceof MediaElementRef) {
			MediaElementRef elementRefDTO = (MediaElementRef) objRef;
			String type = elementRefDTO.getType();
			clazz = mediaElementClassStore().get(type);
			if (clazz == null) {
				clazz = MediaElementImpl.class;
			}

		} else if (objRef instanceof MediaMixerRef) {
			MediaMixerRef mixerRefDTO = (MediaMixerRef) objRef;
			String type = mixerRefDTO.getType();
			clazz = mediaElementClassStore().get(type);

			if (clazz == null) {
				clazz = MediaMixer.class;
			}

		} else if (objRef instanceof MediaPipelineRef) {
			clazz = MediaPipelineImpl.class;

		} else {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(
					"Unknown object ref of type " + objRef.getClass(), 30000);
		}

		MediaObject obj = instantiateMediaObject(clazz, objRef);

		return obj;
	}

	private static Class<?> classFromPadDirection(
			KmsMediaPadDirection padDirection) {
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
			MediaObjectRef objRef) {

		// TODO Change error code
		Assert.notNull(clazz, "MediaObject class not found", 30000);

		MediaObject obj;
		try {
			// TODO: document that all media objects must have such constructor
			Constructor<?> constructor = clazz
					.getConstructor(objRef.getClass());
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
