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

/**
 * Event raised when the stream that the element sends out is finished.. An
 * element receiving this event will generally just process any buffered data,
 * and then forward the event further downstream.
 * 
 * @author Iván Gracia (igracia@gsyc.es)
 * 
 */
public interface EndOfStreamEvent extends MediaEvent {

	// EOS events have no properties
}
