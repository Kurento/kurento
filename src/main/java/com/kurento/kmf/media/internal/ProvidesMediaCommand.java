package com.kurento.kmf.media.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.kurento.kmf.media.commands.MediaCommandResult;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProvidesMediaCommand {

	public String type();

	/**
	 * This field represents the result class for the command
	 * 
	 * @return
	 */
	public Class<? extends MediaCommandResult> resultClass();
}
