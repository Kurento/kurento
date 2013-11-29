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
package com.kurento.kmf.media.params.internal;

//TODO this should be ADD_NEW_WINDOW_PARAM_WINDOW_TYPE
import static com.kurento.kms.thrift.api.KmsMediaPointerDetectorFilterTypeConstants.ADD_NEW_WINDOW_PARAM_WINDOW;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.internal.ProvidesMediaParam;
import com.kurento.kms.thrift.api.KmsMediaPointerDetectorWindow;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
// TODO this should be ADD_NEW_WINDOW_PARAM_WINDOW_TYPE
@ProvidesMediaParam(type = ADD_NEW_WINDOW_PARAM_WINDOW)
public class PointerDetectorWindowMediaParam extends
		AbstractThriftSerializedMediaParam {

	private final KmsMediaPointerDetectorWindow window = new KmsMediaPointerDetectorWindow();

	public int getUpperRightX() {
		return this.window.topRightCornerX;
	}

	public int getUpperRightY() {
		return this.window.topRightCornerY;
	}

	public int getWidth() {
		return this.window.width;
	}

	public int getHeight() {
		return this.window.height;
	}

	public String getId() {
		return this.window.id;
	}

	public PointerDetectorWindowMediaParam() {
		super(ADD_NEW_WINDOW_PARAM_WINDOW);
	}

	/**
	 * @param id
	 * @param height
	 * @param width
	 * @param upperRightX
	 * @param upperRightY
	 */
	public PointerDetectorWindowMediaParam(final String id, final int height,
			final int width, final int upperRightX, final int upperRightY) {
		// TODO this should be ADD_NEW_WINDOW_PARAM_WINDOW_TYPE
		this();
		this.window.height = height;
		this.window.id = id;
		this.window.topRightCornerX = upperRightX;
		this.window.topRightCornerY = upperRightY;
		this.window.width = width;
	}

	@Override
	protected TProtocol serializeDataToThrift(TProtocol pr) {

		try {
			window.write(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
		return pr;
	}

	@Override
	protected void deserializeFromTProtocol(final TProtocol pr) {
		final KmsMediaPointerDetectorWindow kmsParam = new KmsMediaPointerDetectorWindow();
		try {
			kmsParam.read(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), 30000);
		}

		window.height = kmsParam.height;
		window.id = kmsParam.id;
		window.topRightCornerX = kmsParam.topRightCornerX;
		window.topRightCornerY = kmsParam.topRightCornerY;
		window.width = kmsParam.width;

	}

	@Override
	public boolean equals(Object obj) {

		if (obj == null) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		if (!obj.getClass().equals(this.getClass())) {
			return false;
		}

		PointerDetectorWindowMediaParam param = (PointerDetectorWindowMediaParam) obj;
		return this.window.equals(param.window);
	}

	@Override
	public int hashCode() {
		return this.window.hashCode();
	}

}
