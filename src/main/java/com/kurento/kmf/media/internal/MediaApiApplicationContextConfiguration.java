package com.kurento.kmf.media.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.commands.MediaCommandResult;
import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kmf.media.internal.refs.MediaMixerRefDTO;
import com.kurento.kmf.media.internal.refs.MediaObjectRefDTO;
import com.kurento.kmf.media.internal.refs.MediaPadRefDTO;
import com.kurento.kmf.media.internal.refs.MediaPipelineRefDTO;
import com.kurento.kmf.media.objects.MediaElement;
import com.kurento.kmf.media.objects.MediaMixer;
import com.kurento.kmf.media.objects.MediaObject;
import com.kurento.kmf.media.objects.MediaPipeline;
import com.kurento.kmf.media.objects.MediaPipelineFactory;
import com.kurento.kmf.media.objects.MediaSink;
import com.kurento.kmf.media.objects.MediaSource;
import com.kurento.kmf.media.pool.MediaServerClientPoolService;
import com.kurento.kms.thrift.api.CommandResult;
import com.kurento.kms.thrift.api.KmsError;
import com.kurento.kms.thrift.api.KmsEvent;
import com.kurento.kms.thrift.api.PadDirection;

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
	MediaCommandResultClassStore mediaCommandResultClassStore() {
		return new MediaCommandResultClassStore();
	}

	@Bean
	@Scope("prototype")
	public MediaEvent mediaEvent(KmsEvent event) {

		Class<?> clazz = mediaEventClassStore().get(event.getType());

		// TODO Change error code
		Assert.notNull(clazz, "MediaEvent class not found", 30000);

		MediaEvent mediaEvent;
		try {
			Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
			// This cast is safe as long as the type of the class refers to a
			// type that extends from MediaObject.
			// Nevertheless, a catch is included in the try-catch block.
			mediaEvent = (MediaEvent) constructor.newInstance(event);
		} catch (ClassCastException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		} catch (InstantiationException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		} catch (IllegalAccessException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		} catch (IllegalArgumentException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		} catch (InvocationTargetException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		} catch (SecurityException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		}

		if (event.isSetData()) {
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
	public MediaCommandResult mediaCommandResult(CommandResult result) {
		Class<?> clazz = mediaCommandResultClassStore().get(result.dataType);

		// TODO Change error code
		Assert.notNull(clazz, "MediaEvent class not found", 30000);
		MediaCommandResult mediaCommandResult;

		try {
			Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
			// This cast is safe as long as the type of the class refers to a
			// type that extends from MediaObject.
			// Nevertheless, a catch is included in the try-catch block.
			mediaCommandResult = (MediaCommandResult) constructor
					.newInstance(result);
		} catch (ClassCastException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		} catch (InstantiationException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		} catch (IllegalAccessException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		} catch (IllegalArgumentException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		} catch (InvocationTargetException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		} catch (SecurityException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		}

		if (result.isSetResult()) {
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
			// TODO maybe this could be included in the classStore as default
			if (clazz == null) {
				clazz = MediaElement.class;
			}
		} else if (objRef instanceof MediaMixerRefDTO) {
			MediaMixerRefDTO mixerRefDTO = (MediaMixerRefDTO) objRef;
			String type = mixerRefDTO.getType();
			clazz = mediaEventClassStore().get(type);

			if (clazz == null) {
				clazz = MediaMixer.class;
			}
		} else if (objRef instanceof MediaPipelineRefDTO) {
			clazz = MediaPipeline.class;
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
			clazz = MediaSink.class;
			break;
		case SRC:
			clazz = MediaSource.class;
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
			Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
			// This cast is safe as long as the type of the class refers to a
			// type that extends from MediaObject.
			// Nevertheless, a catch is included in the try-catch block.
			obj = (MediaObject) constructor.newInstance(objRef);
		} catch (ClassCastException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		} catch (InstantiationException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		} catch (IllegalAccessException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		} catch (IllegalArgumentException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		} catch (InvocationTargetException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		} catch (SecurityException e) {
			// TODO error code and message
			throw new KurentoMediaFrameworkException(" ", e, 30000);
		}

		return obj;
	}

}
