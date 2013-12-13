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

import com.kurento.kmf.media.events.HasPlateDetectedListener;

/**
 * PlateDetectorFilter interface. This type of {@code Endpoint} detects vehicle
 * plates in a video feed.
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.1
 */

public interface PlateDetectorFilter extends Filter, HasPlateDetectedListener {

	/**
	 * Builder for the {@link PlateDetectorFilter}.
	 * 
	 * @author Ivan Gracia (igracia@gsyc.es)
	 * @since 2.0.1
	 */
	public interface PlateDetectorFilterBuilder extends
			MediaObjectBuilder<PlateDetectorFilterBuilder, PlateDetectorFilter> {
		// No special method for the builder at this level
	}

}
