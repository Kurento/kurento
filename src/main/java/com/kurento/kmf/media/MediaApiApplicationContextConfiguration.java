package com.kurento.kmf.media;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.kurento.kms.api.MediaObjectId;

@Configuration
class MediaApiApplicationContextConfiguration {

	@Bean
	MediaPipelineFactory mediaPipelineFactory() {
		return new MediaPipelineFactory();
	}

	@Bean
	MediaServerServiceManager mediaServerServiceManager() {
		return new MediaServerServiceManager();
	}

	@Bean
	MediaHandlerServer mediaHandlerServer() {
		return new MediaHandlerServer();
	}

	@Bean
	MediaServerHandler mediaServerHandler() {
		return new MediaServerHandler();
	}

	@Bean
	@Scope("prototype")
	MediaPipeline mediaPipeline(MediaObjectId mediaPipelineId) {
		return new MediaPipeline(mediaPipelineId);
	}

	@Bean
	@Scope("prototype")
	<T extends MediaObject> T mediaObject(Class<T> type,
			MediaObjectId mediaObjectId) {
		try {
			Constructor<T> constructor = type
					.getDeclaredConstructor(MediaObjectId.class);
			return constructor.newInstance(mediaObjectId);
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		} catch (InstantiationException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
