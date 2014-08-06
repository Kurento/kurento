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
package org.kurento.tool.rom.test.model.client;

import org.kurento.media.MediaElement;
import org.kurento.media.events.MediaEvent;

/**
 * Interface to be implemented by objects that represent the registration of a
 * listener in the system. Implementers of this interface may be used by the
 * system to track listeners of events registered by users. Subscribing to a
 * certain {@link MediaEvent} raised by a {@link MediaElement} generates a
 * {@code ListenerRegistration}, that can be used by the client to unregister
 * this listener.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface ListenerRegistration {

	/**
	 * Returns the registration id for this listener
	 * 
	 * @return The id
	 */
	String getRegistrationId();

}
