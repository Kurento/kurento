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

import static com.kurento.kms.thrift.api.KmsMediaPointerDetectorFilterTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE;

import java.util.HashSet;
import java.util.Set;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.internal.ProvidesMediaParam;
import com.kurento.kms.thrift.api.KmsMediaPointerDetectorWindow;
import com.kurento.kms.thrift.api.KmsMediaPointerDetectorWindowSet;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
@ProvidesMediaParam(type = CONSTRUCTOR_PARAMS_DATA_TYPE)
public final class PointerDetectorConstructorParam extends
		AbstractThriftSerializedMediaParam {

	private final Set<PointerDetectorWindow> windows = new HashSet<PointerDetectorWindow>();

	public Set<PointerDetectorWindow> getWindows() {
		return this.windows;
	}

	public void addDetectorWindow(final String id, final int height,
			final int width, final int upperRightX, final int upperRightY) {
		final PointerDetectorWindow window = new PointerDetectorWindow(id,
				height, width, upperRightX, upperRightY);
		windows.add(window);
	}

	public PointerDetectorConstructorParam() {
		super(CONSTRUCTOR_PARAMS_DATA_TYPE);
	}

	@Override
	protected TProtocol serializeDataToThrift(final TProtocol pr) {
		final KmsMediaPointerDetectorWindowSet kmsWindowSet = new KmsMediaPointerDetectorWindowSet();
		kmsWindowSet.setWindows(new HashSet<KmsMediaPointerDetectorWindow>(
				windows.size()));

		for (final PointerDetectorWindow window : windows) {
			final KmsMediaPointerDetectorWindow kmsWindow = new KmsMediaPointerDetectorWindow();
			kmsWindow.height = window.getHeight();
			kmsWindow.id = window.getId();
			kmsWindow.topRightCornerX = window.getUpperRightX();
			kmsWindow.topRightCornerY = window.getUpperRightY();
			kmsWindow.width = window.getWidth();
			kmsWindowSet.addToWindows(kmsWindow);
		}

		try {
			kmsWindowSet.write(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
		return pr;
	}

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		final KmsMediaPointerDetectorWindowSet kmsParams = new KmsMediaPointerDetectorWindowSet();
		try {
			kmsParams.read(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), 30000);
		}

		for (final KmsMediaPointerDetectorWindow kmsWindow : kmsParams
				.getWindows()) {
			final PointerDetectorWindow window = new PointerDetectorWindow(
					kmsWindow);
			windows.add(window);
		}

	}

	private static final class PointerDetectorWindow {

		private final int upperRightX;

		private final int upperRightY;

		private final int width;

		private final int height;

		private final String id;

		public int getUpperRightX() {
			return upperRightX;
		}

		public int getUpperRightY() {
			return upperRightY;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public String getId() {
			return id;
		}

		/**
		 * @param type
		 */
		PointerDetectorWindow(KmsMediaPointerDetectorWindow kmsWindow) {
			this(kmsWindow.id, kmsWindow.height, kmsWindow.width,
					kmsWindow.topRightCornerX, kmsWindow.topRightCornerY);
		}

		/**
		 * @param type
		 */
		PointerDetectorWindow(String id, int height, int width,
				int upperRightX, int upperRightY) {
			this.height = height;
			this.id = id;
			this.upperRightX = upperRightX;
			this.upperRightY = upperRightY;
			this.width = width;
		}

	}
}
