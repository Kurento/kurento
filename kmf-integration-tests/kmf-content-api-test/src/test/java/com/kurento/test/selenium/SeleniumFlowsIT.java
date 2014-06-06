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
package com.kurento.test.selenium;

import static com.kurento.demo.internal.EventListener.HANDLER_ON_CONTENT_COMMAND;
import static com.kurento.demo.internal.EventListener.HANDLER_ON_CONTENT_REQUEST;
import static com.kurento.demo.internal.EventListener.HANDLER_ON_CONTENT_STARTED;
import static com.kurento.demo.internal.EventListener.HANDLER_ON_SESSION_ERROR;
import static com.kurento.demo.internal.EventListener.HANDLER_ON_SESSION_TERMINATED;
import static com.kurento.demo.internal.EventListener.HANDLER_ON_UNCAUGHT_EXCEPTION;
import static com.kurento.demo.internal.EventListener.JS_ON_ERROR;
import static com.kurento.demo.internal.EventListener.JS_ON_REMOTE_STREAM;
import static com.kurento.demo.internal.EventListener.JS_ON_START;
import static com.kurento.demo.internal.EventListener.JS_ON_TERMINATE;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * Selenium tests for assessment of rigth KMF flows (JavaScript and Handlers);
 * these tests uses specific test handlers.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 */
@RunWith(Arquillian.class)
public class SeleniumFlowsIT extends BaseSeleniumTst {

	/**
	 * Flow test 1: Playing a video with a correct URL.
	 * 
	 * @param driverClass
	 *            Driver class (Firefox | Chrome)
	 * @param handler
	 *            HTTP Player handler path
	 * @param video
	 *            contentId of the video to be played
	 * @throws Exception
	 */
	public void testFlowPlayOk(Class<? extends WebDriver> driverClass,
			String handler, String video) throws Exception {
		// TEST DATA
		// Expected flows
		String[] expectedHandlerFlow = { HANDLER_ON_CONTENT_REQUEST,
				HANDLER_ON_CONTENT_STARTED, HANDLER_ON_SESSION_TERMINATED };
		String[] expectedJavaScriptFlow = { JS_ON_REMOTE_STREAM, JS_ON_START,
				JS_ON_TERMINATE };
		final String[] expectedEvents = {};

		// TEST EXERCISE
		seleniumTest(driverClass, handler, video, expectedHandlerFlow,
				expectedJavaScriptFlow, expectedEvents);
	}

	@Test
	public void testFlowPlayOkChrome() throws Exception {
		testFlowPlayOk(ChromeDriver.class, "player-json-tunnel", "mp4");
	}

	@Test
	public void testFlowPlayOkFirefox() throws Exception {
		testFlowPlayOk(FirefoxDriver.class, "player-json-redirect", "mkv");
	}

	/**
	 * Flow test 2: Playing a video with a MediaElement which does not exist on
	 * the server.
	 * 
	 * @param driverClass
	 *            Driver class (Firefox | Chrome)
	 * @throws Exception
	 */
	public void testFlowPlayBadMediaElement(
			Class<? extends WebDriver> driverClass) throws Exception {
		// TEST DATA
		// Handler
		final String handlerPath = "playerFlowBadMediaElement";
		// Expected flows
		final String[] expectedHandlerFlow = { HANDLER_ON_CONTENT_REQUEST,
				HANDLER_ON_CONTENT_STARTED, HANDLER_ON_SESSION_ERROR };
		final String[] expectedJavaScriptFlow = { JS_ON_REMOTE_STREAM,
				JS_ON_START, JS_ON_ERROR };
		final String[] expectedEvents = {};

		// TEST EXERCISE
		seleniumTest(driverClass, handlerPath, null, expectedHandlerFlow,
				expectedJavaScriptFlow, expectedEvents);
	}

	@Test
	public void testFlowPlayBadMediaElementChrome() throws Exception {
		testFlowPlayBadMediaElement(ChromeDriver.class);
	}

	@Test
	public void testFlowPlayBadMediaElementFirefox() throws Exception {
		testFlowPlayBadMediaElement(FirefoxDriver.class);
	}

	/**
	 * Flow test 3: Playing a video with a incorrect URL.
	 * 
	 * @param driverClass
	 *            Driver class (Firefox | Chrome)
	 * @throws Exception
	 */
	public void testFlowPlayBadUrl(Class<? extends WebDriver> driverClass)
			throws Exception {
		// TEST DATA
		// Handler
		final String handlerPath = "playerFlowBad";
		// Expected flows
		final String[] expectedHandlerFlow = { HANDLER_ON_CONTENT_REQUEST,
				HANDLER_ON_CONTENT_STARTED, HANDLER_ON_SESSION_ERROR };
		final String[] expectedJavaScriptFlow = { JS_ON_REMOTE_STREAM,
				JS_ON_START, JS_ON_ERROR };
		final String[] expectedEvents = {};

		// TEST EXERCISE
		seleniumTest(driverClass, handlerPath, null, expectedHandlerFlow,
				expectedJavaScriptFlow, expectedEvents);
	}

	@Test
	public void testFlowPlayBadUrlChrome() throws Exception {
		testFlowPlayBadUrl(ChromeDriver.class);
	}

	@Test
	public void testFlowPlayBadUrlFirefox() throws Exception {
		testFlowPlayBadUrl(FirefoxDriver.class);
	}

	/**
	 * Flow test 4: Handler throws Exception in onContentRequest.
	 * 
	 * @param driverClass
	 *            Driver class (Firefox | Chrome)
	 * @throws Exception
	 */
	public void testFlowException(Class<? extends WebDriver> driverClass)
			throws Exception {
		// TEST DATA
		// Handler
		final String handlerPath = "playerFlowOkWithExcp";
		// Expected flows
		final String[] expectedHandlerFlow = { HANDLER_ON_CONTENT_REQUEST,
				HANDLER_ON_UNCAUGHT_EXCEPTION, HANDLER_ON_CONTENT_STARTED,
				HANDLER_ON_SESSION_TERMINATED };
		final String[] expectedJavaScriptFlow = { JS_ON_REMOTE_STREAM,
				JS_ON_START, JS_ON_TERMINATE };
		final String[] expectedEvents = {};

		// TEST EXERCISE
		seleniumTest(driverClass, handlerPath, null, expectedHandlerFlow,
				expectedJavaScriptFlow, expectedEvents);
	}

	@Test
	public void testFlowExceptionChrome() throws Exception {
		testFlowException(ChromeDriver.class);
	}

	@Test
	public void testFlowExceptionFirefox() throws Exception {
		testFlowException(FirefoxDriver.class);
	}

	/**
	 * Flow test 5: Handler does anything (timeout should be reached)
	 * 
	 * @param driverClass
	 *            Driver class (Firefox | Chrome)
	 * @throws Exception
	 */
	public void testFlowNothing(Class<? extends WebDriver> driverClass)
			throws Exception {
		// TEST DATA
		// Handler
		final String handlerPath = "playerFlowNothing";
		// Expected flows
		final String[] expectedHandlerFlow = { HANDLER_ON_CONTENT_REQUEST };
		final String[] expectedJavaScriptFlow = { JS_ON_ERROR };
		final String[] expectedEvents = {};

		// TEST EXERCISE
		seleniumTest(driverClass, handlerPath, null, expectedHandlerFlow,
				expectedJavaScriptFlow, expectedEvents);
	}

	@Test
	public void testFlowNothingChrome() throws Exception {
		testFlowNothing(ChromeDriver.class);
	}

	@Test
	public void testFlowNothingFirefox() throws Exception {
		testFlowNothing(FirefoxDriver.class);
	}

	/**
	 * Flow test 6: 2 commands are sent from JavaScript. One of these commands
	 * should interpreted in handler as an error, and an exception should be
	 * raised.
	 * 
	 * @param driverClass
	 *            Driver class (Firefox | Chrome)
	 * @param handler
	 *            HTTP Player handler path
	 * @throws Exception
	 */
	public void testFlowCommands(Class<? extends WebDriver> driverClass,
			String handler) throws Exception {
		// TEST DATA
		// Expected flows
		final String[] expectedHandlerFlow = { HANDLER_ON_CONTENT_REQUEST,
				HANDLER_ON_CONTENT_STARTED, HANDLER_ON_CONTENT_COMMAND,
				HANDLER_ON_UNCAUGHT_EXCEPTION, HANDLER_ON_SESSION_TERMINATED };
		final String[] expectedJavaScriptFlow = { JS_ON_REMOTE_STREAM,
				JS_ON_START, JS_ON_TERMINATE };
		final String[] expectedEvents = {};

		// TEST EXERCISE
		seleniumTest(driverClass, handler, null, expectedHandlerFlow,
				expectedJavaScriptFlow, expectedEvents);
	}

	@Test
	public void testFlowCommandsChrome() throws Exception {
		testFlowCommands(ChromeDriver.class, "player-json-redirect");
	}

	@Test
	public void testFlowCommandsFirefox() throws Exception {
		testFlowCommands(FirefoxDriver.class, "player-json-tunnel");
	}

}
