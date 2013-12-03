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
package com.kurento.kmf.media.events;

import com.kurento.kmf.media.ZBarFilter;

/**
 * Event raised by a {@link ZBarFilter} when a code is found in the data
 * streamed.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface CodeFoundEvent extends MediaEvent {

	/**
	 * Type of code found
	 * 
	 * @return The type of code
	 */
	String getCodeType();

	/**
	 * Gets the value contained in the code
	 * 
	 * @return The value
	 */
	String getValue();
}
