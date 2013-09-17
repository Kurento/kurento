package com.kurento.kmf.media.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.common.excption.internal.ReflectionUtils;
import com.kurento.kmf.media.IsMediaElement;
import com.kurento.kmf.media.IsMediaEvent;
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

	// TODO review
	@Bean
	@Scope("prototype")
	public MediaPipeline mediaPipeline(MediaPipelineRefDTO objRef) {
		return new MediaPipeline(objRef);
	}

	// TODO review

	@Bean
	@Scope("prototype")
	public MediaObject mediaObject(MediaObjectRefDTO objRef) {

		// This cast is safe as long as the type of the class refers to a
		// type that extends from MediaObject.
		// Nevertheless, a catch is included in the try-catch block.
		MediaObject obj;

		if (objRef instanceof MediaPadRefDTO) {

			MediaPadRefDTO padRefDTO = (MediaPadRefDTO) objRef;
			// TODO instantiate one Pad depending on type
			switch (padRefDTO.getPadDirection()) {
			case SINK:
				obj = new MediaSink(padRefDTO);
				break;
			case SRC:
				obj = new MediaSource(padRefDTO);
				break;
			default:
				throw new IllegalArgumentException("Invalid MediaPad direction");
			}
		} else if (objRef instanceof MediaElementRefDTO) {
			MediaElementRefDTO elementRefDTO = (MediaElementRefDTO) objRef;
			String type = elementRefDTO.getType();
			obj = (MediaObject) instantiateMediaObject(type, objRef);
		} else if (objRef instanceof MediaMixerRefDTO) {
			MediaMixerRefDTO mixerRefDTO = (MediaMixerRefDTO) objRef;
			String type = mixerRefDTO.getType();
			obj = (MediaObject) instantiateMediaObject(type, objRef);
		} else if (objRef instanceof MediaPipelineRefDTO) {
			// TODO is this ok?
			obj = mediaPipeline((MediaPipelineRefDTO) objRef);
		} else {
			throw new IllegalArgumentException(
					"objRef is not of any known type");
		}

		return obj;
	}

	@Bean
	@Scope("prototype")
	public KmsEvent mediaEvent(MediaEvent event) {

		// This cast is safe as long as the type of the class refers to a
		// type that extends from MediaObject.
		// Nevertheless, a catch is included in the try-catch block.
		Set<Class<?>> annotatedClassSet = ReflectionUtils
				.getTypesAnnotatedWith(IsMediaEvent.class);

		Class<?> mediaEventClass = null;

		for (Class<?> clazz : annotatedClassSet) {
			IsMediaEvent annotation = clazz.getAnnotation(IsMediaEvent.class);
			if (annotation.type().equals(event.getType())) {
				mediaEventClass = clazz;
				break;
			}
		}

		if (mediaEventClass == null) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(
					"MediaEvent class not found", 3000);
		}

		try {
			Constructor<?> constructor = mediaEventClass
					.getDeclaredConstructors()[0];
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

	private MediaObject instantiateMediaObject(String type,
			MediaObjectRefDTO objRef) {
		Set<Class<?>> annotatedClassSet = ReflectionUtils
				.getTypesAnnotatedWith(IsMediaElement.class);

		Class<?> mediaObjectClass = null;

		for (Class<?> clazz : annotatedClassSet) {
			IsMediaElement annotation = clazz
					.getAnnotation(IsMediaElement.class);
			if (annotation.type().equals(type)) {
				mediaObjectClass = clazz;
				break;
			}
		}

		if (mediaObjectClass == null) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(
					"MediaElement class not found", 3000);
		}

		try {
			Constructor<?> constructor = mediaObjectClass
					.getDeclaredConstructors()[0];
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
