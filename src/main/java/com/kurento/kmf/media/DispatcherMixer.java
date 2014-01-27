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
package com.kurento.kmf.media;


//TODO what info should we include about the main mixer?
/**
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface DispatcherMixer extends MediaMixer {

	void setMainEndPoint(MixerPort mixerEndpoint);

	void unsetMainEndPoint();

	void setMainEndPoint(MixerPort mixerEndpoint, Continuation<Void> cont);

	void unsetMainEndPoint(Continuation<Void> cont);

	/**
	 * Builder for the {@link DispatcherMixer}.
	 * 
	 * @author Ivan Gracia (igracia@gsyc.es)
	 * @since 2.0.0
	 */
	public interface DispatcherMixerBuilder extends
			MediaObjectBuilder<DispatcherMixerBuilder, DispatcherMixer> {

	}

}
