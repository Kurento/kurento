package com.kurento.kmf.media.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.media.events.KmsEvent;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kmf.media.internal.refs.MediaMixerRefDTO;
import com.kurento.kmf.media.internal.refs.MediaObjectRefDTO;
import com.kurento.kmf.media.internal.refs.MediaPadRefDTO;
import com.kurento.kmf.media.internal.refs.MediaPipelineRefDTO;
import com.kurento.kmf.media.objects.MediaObject;
import com.kurento.kmf.media.objects.MediaPipeline;
import com.kurento.kmf.media.objects.MediaPipelineFactory;
import com.kurento.kmf.media.objects.MediaSink;
import com.kurento.kmf.media.objects.MediaSource;
import com.kurento.kmf.media.pool.MediaServerClientPoolService;
import com.kurento.kms.thrift.api.MediaEvent;

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
	public KmsEvent mediaEvent(MediaEvent event) {

		// This cast is safe as long as the type of the class refers to a
		// type that extends from MediaObject.
		// Nevertheless, a catch is included in the try-catch block.
		Class<?> clazz = mediaEventClassStore().get(event.getType());

		// TODO Change error code
		Assert.notNull(clazz, "MediaEvent class not found", 30000);

		try {
			Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
			return (KmsEvent) constructor.newInstance(event);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(e);
		} catch (InstantiationException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Bean
	@Scope("prototype")
	public MediaObject mediaObject(MediaObjectRefDTO objRef) {

		// This cast is safe as long as the type of the class refers to a
		// type that extends from MediaObject.
		// Nevertheless, a catch is included in the try-catch block.
		MediaObject obj;
		Class<?> clazz;
		if (objRef instanceof MediaPadRefDTO) {

			MediaPadRefDTO padRefDTO = (MediaPadRefDTO) objRef;
			switch (padRefDTO.getPadDirection()) {
			case SINK:
				clazz = MediaSink.class;
				break;
			case SRC:
				clazz = MediaSource.class;
				break;
			default:
				throw new IllegalArgumentException("Invalid MediaPad direction");
			}
		} else if (objRef instanceof MediaElementRefDTO) {
			MediaElementRefDTO elementRefDTO = (MediaElementRefDTO) objRef;
			String type = elementRefDTO.getType();
			clazz = mediaEventClassStore().get(type);
		} else if (objRef instanceof MediaMixerRefDTO) {
			MediaMixerRefDTO mixerRefDTO = (MediaMixerRefDTO) objRef;
			String type = mixerRefDTO.getType();
			clazz = mediaEventClassStore().get(type);
		} else if (objRef instanceof MediaPipelineRefDTO) {
			clazz = MediaPipeline.class;
		} else {
			throw new IllegalArgumentException(
					"objRef is not of any known type");
		}

		obj = (MediaObject) instantiateMediaObject(clazz, objRef);
		return obj;
	}

	private MediaObject instantiateMediaObject(Class<?> clazz,
			MediaObjectRefDTO objRef) {

		// TODO Change error code
		Assert.notNull(clazz, "MediaObject class not found", 30000);

		try {
			Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
			return (MediaObject) constructor.newInstance(objRef);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(e);
		} catch (InstantiationException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
