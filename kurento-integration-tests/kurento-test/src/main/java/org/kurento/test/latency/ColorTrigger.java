/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package org.kurento.test.latency;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.List;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread to detect change of color in one of the video tags (local or remote)
 * of the browser.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class ColorTrigger implements Runnable {

	public Logger log = LoggerFactory.getLogger(ColorTrigger.class);
	private VideoTag videoTag;
	private JavascriptExecutor js;
	private Color color = Color.BLACK; // Initial color
	private ChangeColorObservable observable;
	private long timeoutSeconds;

	public ColorTrigger(VideoTag videoTag, JavascriptExecutor js,
			ChangeColorObservable observable, long timeoutSeconds) {
		this.videoTag = videoTag;
		this.js = js;
		this.observable = observable;
		this.timeoutSeconds = timeoutSeconds;
	}

	@Override
	public void run() {
		while (true) {
			try {
				(new WebDriverWait((WebDriver) js, timeoutSeconds))
						.until(new ExpectedCondition<Boolean>() {
							public Boolean apply(WebDriver d) {
								return !((JavascriptExecutor) d).executeScript(
										videoTag.getColor()).equals(color);
							}
						});

				@SuppressWarnings("unchecked")
				Color currentColor = getColor((List<Long>) js
						.executeScript(videoTag.getColor()));

				if (!currentColor.equals(color)) {
					long changeTimeMilis = (Long) js.executeScript(videoTag
							.getTime());
					String parsedtime = new SimpleDateFormat("mm:ss.SSS")
							.format(changeTimeMilis);

					log.debug("Color changed on {} from {} to {} at minute {}",
							videoTag, color, currentColor, parsedtime);
					color = currentColor;

					ChangeColorEvent event = new ChangeColorEvent(videoTag,
							changeTimeMilis, color);
					observable.detectedColorChange(event);
				}
			} catch (WebDriverException we) {
				// This kind of exception can occur but does not matter for the
				// execution of the test
			} catch (Exception e) {
				break;
			}
		}
	}

	private Color getColor(List<Long> color) {
		return new Color(color.get(0).intValue(), color.get(1).intValue(),
				color.get(2).intValue());
	}
}
