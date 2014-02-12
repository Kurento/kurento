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

import static com.kurento.kms.thrift.api.KmsMediaObjectConstants.CONSTRUCTOR_PARAMS_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaServerConstants.DEFAULT_GARBAGE_COLLECTOR_PERIOD;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.internal.refs.MediaObjectRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.MediaObjectConstructorParam;
import com.kurento.kmf.thrift.internal.DistributedGarbageCollector;
import com.kurento.kms.thrift.api.KmsMediaServerConstants;

/**
 * Abstract class that encapsulates the registration of a {@link MediaObject} in
 * the distributed garbage collector. Objects that extend this class will be
 * registered in the DGC, and periodical keepalives will be sent to the Media
 * Server
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
public abstract class AbstractCollectableMediaObject extends
		AbstractMediaObject {

	@Autowired
	private DistributedGarbageCollector distributedGarbageCollector;

	private final int garbagePeriod;

	/**
	 * Default constructor. The distributed garbage collector will be configured
	 * with an interval defined in
	 * {@link KmsMediaServerConstants#DEFAULT_GARBAGE_COLLECTOR_PERIOD}
	 * 
	 * @param ref
	 */
	public AbstractCollectableMediaObject(MediaObjectRef ref) {
		this(ref, DEFAULT_GARBAGE_COLLECTOR_PERIOD);
	}

	/**
	 * Constructor intended to be used by extending classes that wish to
	 * configure the garbage period. This is convenient for developers that want
	 * to be relieved from having to create the map of constructor parameters.
	 * 
	 * @param ref
	 *            reference to the object.
	 * @param garbagePeriod
	 *            the desired garbage period. 0 means the object will be
	 *            excluded from the garbage collector.
	 */
	protected AbstractCollectableMediaObject(MediaObjectRef ref,
			int garbagePeriod) {
		super(ref);
		this.garbagePeriod = garbagePeriod;
	}

	/**
	 * @param ref
	 * @param params
	 */
	public AbstractCollectableMediaObject(MediaObjectRef ref,
			Map<String, MediaParam> params) {
		super(ref, params);
		MediaObjectConstructorParam objConstructorParam = (MediaObjectConstructorParam) params
				.get(CONSTRUCTOR_PARAMS_DATA_TYPE);
		if (objConstructorParam != null) {
			this.garbagePeriod = objConstructorParam
					.getGarbageCollectorPeriod();
		} else {
			this.garbagePeriod = DEFAULT_GARBAGE_COLLECTOR_PERIOD;
		}
	}

	/**
	 * Set a garbage period if none is found in the params map.
	 * 
	 * @param params
	 *            parameters used to build the object
	 * @param garbagePeriod
	 *            the garbage period to use if not previously set.
	 * @return The map of parameters, with the garbage period set.
	 */
	protected static Map<String, MediaParam> setDefaultGarbagePeriodParam(
			Map<String, MediaParam> params, int garbagePeriod) {
		MediaObjectConstructorParam objConstructorParam = (MediaObjectConstructorParam) params
				.get(CONSTRUCTOR_PARAMS_DATA_TYPE);
		if (objConstructorParam == null) {
			objConstructorParam = new MediaObjectConstructorParam();
			objConstructorParam.setGarbageCollectorPeriod(garbagePeriod);
			params.put(CONSTRUCTOR_PARAMS_DATA_TYPE, objConstructorParam);
		}

		return params;
	}

	@Override
	protected void init() {
		if (garbagePeriod > 0) {
			distributedGarbageCollector.registerReference(
					objectRef.getThriftRef(), garbagePeriod);
		}

		super.init();
	}

	@Override
	public void release() {
		distributedGarbageCollector.removeReference(objectRef.getThriftRef());
		super.release();
	}

	@Override
	public void release(final Continuation<Void> cont) {
		distributedGarbageCollector.removeReference(objectRef.getThriftRef());
		super.release(cont);
	}

	@Override
	protected void finalize() {
		distributedGarbageCollector.removeReference(objectRef.getThriftRef());
	}

	protected static abstract class AbstractCollectableMediaObjectBuilder<T extends AbstractCollectableMediaObjectBuilder<T, E>, E extends MediaObject>
			extends AbstractMediaObjectBuilder<T, E> {

		private final MediaObjectConstructorParam param = new MediaObjectConstructorParam();

		protected AbstractCollectableMediaObjectBuilder(
				final String elementType, final MediaPipeline pipeline,
				final int garbagePeriod) {
			super(elementType, pipeline);
			setGarbagePeriod(garbagePeriod);
		}

		public final T withGarbagePeriod(int period) {
			setGarbagePeriod(period);
			return self();
		}

		private void setGarbagePeriod(int period) {
			param.setGarbageCollectorPeriod(period);
			params.put(CONSTRUCTOR_PARAMS_DATA_TYPE, param);
		}

	}

}
