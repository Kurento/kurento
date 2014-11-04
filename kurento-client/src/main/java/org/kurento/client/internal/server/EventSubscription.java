package org.kurento.client.internal.server;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.kurento.client.Event;

@Retention(RetentionPolicy.RUNTIME)
public @interface EventSubscription {
	Class<? extends Event> value();
}