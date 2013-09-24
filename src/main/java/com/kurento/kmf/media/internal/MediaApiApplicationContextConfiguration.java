package com.kurento.kmf.media.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.media.commands.MediaCommand;
import com.kurento.kmf.media.commands.MediaCommandResult;
import com.kurento.kmf.media.events.MediaEvent;
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
import com.kurento.kms.thrift.api.Command;
import com.kurento.kms.thrift.api.CommandResult;
import com.kurento.kms.thrift.api.Event;
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
	public MediaEvent mediaEvent(Event event) {

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
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		} catch (InstantiationException e) {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		} catch (SecurityException e) {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		}

		if (event.isSetData()) {
			mediaEvent.deserializeData(event);
		}

		return mediaEvent;
	}

	@Bean
	@Scope("prototype")
	public MediaCommand mediaCommand(Command command) {

		Class<?> clazz = mediaEventClassStore().get(command.getType());

		// TODO Change error code
		Assert.notNull(clazz, "MediaEvent class not found", 30000);

		MediaCommand mediaCommand;

		try {
			Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
			// This cast is safe as long as the type of the class refers to a
			// type that extends from MediaObject.
			// Nevertheless, a catch is included in the try-catch block.
			mediaCommand = (MediaCommand) constructor.newInstance(command);
		} catch (ClassCastException e) {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		} catch (InstantiationException e) {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		} catch (SecurityException e) {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		}

		return mediaCommand;
	}

	@Bean
	@Scope("prototype")
	public MediaCommandResult mediaCommandResult(String type,
			CommandResult result) {
		Class<?> clazz = mediaEventClassStore().get(type);

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
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		} catch (InstantiationException e) {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
		} catch (SecurityException e) {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(e);
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
		} else if (objRef instanceof MediaMixerRefDTO) {
			MediaMixerRefDTO mixerRefDTO = (MediaMixerRefDTO) objRef;
			String type = mixerRefDTO.getType();
			clazz = mediaEventClassStore().get(type);
		} else if (objRef instanceof MediaPipelineRefDTO) {
			clazz = MediaPipeline.class;
		} else {
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException(
					"objRef is not of any known type");
		}

		MediaObject obj = (MediaObject) instantiateMediaObject(clazz, objRef);
		return obj;
	}

	private Class<?> classFromPadDirection(PadDirection padDirection) {
		Class<?> clazz;
		switch (padDirection) {
		case SINK:
			clazz = MediaSink.class;
			break;
		case SRC:
			clazz = MediaSource.class;
			break;
		default:
			// TODO is this the appropriate exception?
			throw new IllegalArgumentException("Invalid MediaPad direction");
		}
		return clazz;
	}

	private MediaObject instantiateMediaObject(Class<?> clazz,
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

		return obj;
	}

}
