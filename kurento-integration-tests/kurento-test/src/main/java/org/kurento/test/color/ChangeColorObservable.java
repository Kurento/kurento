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
package org.kurento.test.color;

import java.util.Observable;
import java.util.Observer;

/**
 * Change color observable (notifies observers when a color change is detected
 * in the browser).
 *
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class ChangeColorObservable extends Observable {

	@SuppressWarnings("unchecked")
	public void addListener(
			final ChangeColorEventListener<? extends ChangeColorEvent> listener) {
		addObserver(new Observer() {
			public void update(Observable obs, Object e) {
				ChangeColorEvent changeColorEvent = (ChangeColorEvent) e;
				((ChangeColorEventListener<ChangeColorEvent>) listener)
						.onEvent(changeColorEvent);
			}
		});
	}

	public void detectedColorChange(ChangeColorEvent event) {
		setChanged();
		notifyObservers(event);
	}

}
