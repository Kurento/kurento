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
package com.kurento.demo.internal;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener singleton; this class is used in handlers for storing the
 * order of events. This order will be stored in a LinkedList called eventList,
 * which will be used for testing purposes.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 */
public class EventListener {

	private static EventListener singleton = null;

	private static final Logger log = LoggerFactory
			.getLogger(EventListener.class);

	// Handler events
	public static String HANDLER_ON_CONTENT_REQUEST = "onContentRequest";
	public static String HANDLER_ON_CONTENT_STARTED = "onContentStarted";
	public static String HANDLER_ON_SESSION_TERMINATED = "onSessionTerminated";
	public static String HANDLER_ON_CONTENT_COMMAND = "onContentCommand";
	public static String HANDLER_ON_SESSION_ERROR = "onSessionError";
	public static String HANDLER_ON_UNCAUGHT_EXCEPTION = "onUncaughtException";

	// JavaScript events
	public static String JS_ON_START = "onstart";
	public static String JS_ON_TERMINATE = "onterminate";
	public static String JS_ON_LOCAL_STREAM = "onlocalstream";
	public static String JS_ON_REMOTE_STREAM = "onremotestream";
	public static String JS_ON_MEDIA_EVENT = "onmediaevent";
	public static String JS_ON_ERROR = "onerror";

	/**
	 * Ordered String List with the events raised in this handler. Each handler
	 * method (onContentRequest, onContentStarted, ...) should call
	 * addEventList() method adding its reference in the list.
	 */
	public List<String> eventList;

	/**
	 * Default constructor.
	 */
	public EventListener() {
		eventList = new LinkedList<String>();
	}

	/**
	 * Singleton getter.
	 * 
	 * @return EventListener singleton
	 */
	public static EventListener getSingleton() {
		if (singleton == null) {
			singleton = new EventListener();
		}
		return singleton;
	}

	/**
	 * Reads the event list.
	 * 
	 * @return Ordered event list (LinkedList)
	 */
	public static List<String> getEventList() {
		return EventListener.getSingleton().eventList;
	}

	/**
	 * Reset the event list.
	 */
	public static void clearEventList() {
		EventListener.getSingleton().eventList.clear();
	}

	/**
	 * Add event name to static field eventList, in to order to verify the flow
	 * order in Selenium test cases.
	 */
	public static void addEvent() {
		// Using stack trace to find out the name of the invoking method
		final StackTraceElement[] stackTraceElements = Thread.currentThread()
				.getStackTrace();

		// In stackTraceElements array:
		// - position 0 is for getStackTrace()
		// - position 1 is for addEventList()
		// - position 2 if for invoking method
		final String invokingMethod = stackTraceElements[2].getMethodName();
		EventListener.getSingleton().eventList.add(invokingMethod);
		log.info("********* Handler Event: " + invokingMethod);
	}

}
