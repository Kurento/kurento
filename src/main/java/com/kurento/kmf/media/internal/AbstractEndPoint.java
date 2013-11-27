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
package com.kurento.kmf.media.internal;

import java.util.Map;

import com.kurento.kmf.media.EndPoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.params.MediaParam;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
public abstract class AbstractEndPoint extends MediaElementImpl implements
		EndPoint {

	/**
	 * @param objectRef
	 */
	public AbstractEndPoint(MediaElementRef objectRef) {
		super(objectRef);
	}

	/**
	 * @param objectRef
	 * @param params
	 */
	public AbstractEndPoint(MediaElementRef objectRef,
			Map<String, MediaParam> params) {
		super(objectRef, params);
	}

	protected static abstract class AbstractEndPointBuilder<T extends AbstractEndPointBuilder<T, E>, E extends EndPoint>
			extends MediaElementBuilderImpl<T, E> {

		/**
		 * @param elementType
		 */
		protected AbstractEndPointBuilder(final String elementType,
				final MediaPipeline pipeline) {
			super(elementType, pipeline);
		}

	}

}
