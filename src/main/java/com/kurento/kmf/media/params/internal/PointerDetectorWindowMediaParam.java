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

import static com.kurento.kms.thrift.api.KmsMediaPointerDetectorFilterTypeConstants.ADD_NEW_WINDOW_PARAM_WINDOW_TYPE;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.internal.ProvidesMediaParam;
import com.kurento.kms.thrift.api.KmsMediaPointerDetectorWindow;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
@ProvidesMediaParam(type = ADD_NEW_WINDOW_PARAM_WINDOW_TYPE)
public class PointerDetectorWindowMediaParam extends
		AbstractThriftSerializedMediaParam {

	private KmsMediaPointerDetectorWindow window;

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

	/**
	 * The uri for the inactive image configured.
	 * <p>
	 * This method never return null
	 * </p>
	 * 
	 * @return object with the uri or an empty uri, if none was configured.
	 */
	public URI getInactiveImageUri() {
		try {
			return this.window.isSetInactiveOverlayImageUri() ? new URI(
					this.window.inactiveOverlayImageUri) : new URI("");
		} catch (URISyntaxException e) {
			// This code should never be reached if the URI received from the
			// KMS is compliant with the URI standard.
			throw new KurentoMediaFrameworkException("Wrong URI format "
					+ this.window.inactiveOverlayImageUri, 30000);
		}
	}

	/**
	 * The uri for the active image configured.
	 * <p>
	 * This method never return null
	 * </p>
	 * 
	 * @return object with the uri or an empty uri, if none was configured.
	 */
	public URI getActiveImageUri() {
		try {
			return this.window.isSetActiveOverlayImageUri() ? new URI(
					this.window.activeOverlayImageUri) : new URI("");
		} catch (URISyntaxException e) {
			// This code should never be reached if the URI received from the
			// KMS is compliant with the URI standard.
			throw new KurentoMediaFrameworkException("Wrong URI format "
					+ this.window.activeOverlayImageUri, 30000);
		}
	}

	public double getImageTransparency() {
		return this.window.overlayTransparency;
	}

	/**
	 * Default constructor. This constructor is intended to be used by the
	 * framework.
	 */
	public PointerDetectorWindowMediaParam() {
		super(ADD_NEW_WINDOW_PARAM_WINDOW_TYPE);
	}

	private PointerDetectorWindowMediaParam(
			PointerDetectorWindowMediaParamBuilder builder) {
		this();
		this.window = builder.window;
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

		this.window = kmsParam.deepCopy();
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

	/**
	 * {@link PointerDetectorWindowMediaParam} builder
	 * 
	 * @author Ivan Gracia (igracia@gsyc.es)
	 * 
	 */
	public static class PointerDetectorWindowMediaParamBuilder {

		protected final KmsMediaPointerDetectorWindow window;

		/**
		 * Constructor for the builder, with the minimum set of attributes to
		 * build a window. If no further configuration is don, the window will
		 * appear outlined as a box.
		 * 
		 * @param id
		 *            id of the window
		 * @param height
		 *            of the window
		 * @param width
		 *            of the window
		 * @param upperRightX
		 *            x coordinate of the upper right corner
		 * @param upperRightY
		 *            y coordinate of the upper right corner
		 */
		public PointerDetectorWindowMediaParamBuilder(final String id,
				final int height, final int width, final int upperRightX,
				final int upperRightY) {
			window = new KmsMediaPointerDetectorWindow(upperRightX,
					upperRightY, width, height, id);
		}

		/**
		 * Sets an image to be shown in the configured window, replacing the
		 * default box-style
		 * 
		 * @param uri
		 *            the uri pointing to the image, accessible to the media
		 *            server.
		 * @return the builder
		 * @throws URISyntaxException
		 *             If the given string violates RFC&nbsp;2396
		 */
		public PointerDetectorWindowMediaParamBuilder withImage(final String uri)
				throws URISyntaxException {
			return this.withImage(new URI(uri));
		}

		/**
		 * Sets an image to be shown in the configured window, replacing the
		 * default box-style
		 * 
		 * @param uri
		 *            the uri pointing to the image, accessible to the media
		 *            server.
		 * @return the builder
		 */
		public PointerDetectorWindowMediaParamBuilder withImage(final URI uri) {
			this.window.activeOverlayImageUri = uri.toString();
			this.window.inactiveOverlayImageUri = uri.toString();
			return this;
		}

		/**
		 * Sets an image to be shown when the pointer is inside the window area.
		 * 
		 * @param uri
		 *            the uri pointing to the image, accessible to the media
		 *            server.
		 * @return the builder
		 * @throws URISyntaxException
		 *             If the given string violates RFC&nbsp;2396
		 */
		public PointerDetectorWindowMediaParamBuilder withActiveImage(
				final String uri) throws URISyntaxException {
			return this.withActiveImage(new URI(uri));
		}

		/**
		 * Sets an image to be shown when the pointer is inside the window area.
		 * 
		 * @param uri
		 *            the uri pointing to the image, accessible to the media
		 *            server.
		 * @return the builder
		 */
		public PointerDetectorWindowMediaParamBuilder withActiveImage(
				final URI uri) {
			this.window.activeOverlayImageUri = uri.toString();
			return this;
		}

		/**
		 * Sets an image to be shown when the pointer is outside the window
		 * area.
		 * 
		 * @param uri
		 * @return the builder
		 * @throws URISyntaxException
		 *             If the given string violates RFC&nbsp;2396
		 */
		public PointerDetectorWindowMediaParamBuilder withInactiveImage(
				final String uri) throws URISyntaxException {
			return this.withInactiveImage(new URI(uri));
		}

		/**
		 * Sets an image to be shown when the pointer is outside the window
		 * area.
		 * 
		 * @param uri
		 *            the uri pointing to the image, accessible to the media
		 *            server.
		 * @return the builder
		 */
		public PointerDetectorWindowMediaParamBuilder withInactiveImage(
				final URI uri) {
			this.window.inactiveOverlayImageUri = uri.toString();
			return this;
		}

		/**
		 * Sets the transparency level of the image.
		 * 
		 * @param transparency
		 *            transparency ranging from 0 to 1. Values closer to 0 make
		 *            the image more opaque.
		 * @return the builder
		 */
		public PointerDetectorWindowMediaParamBuilder withImageTransparency(
				final double transparency) {
			this.window.overlayTransparency = transparency;
			return this;
		}

		/**
		 * Builds a {@link PointerDetectorWindowMediaParam} object
		 * 
		 * @return the object built with the configuration from the builder.
		 */
		public PointerDetectorWindowMediaParam build() {
			return new PointerDetectorWindowMediaParam(this);
		}

	}

}
