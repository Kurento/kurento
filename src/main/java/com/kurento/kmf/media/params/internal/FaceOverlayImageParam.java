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

import static com.kurento.kms.thrift.api.KmsMediaFaceOverlayFilterTypeConstants.SET_IMAGE_OVERLAY_PARAM_IMAGE_TYPE;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.internal.ProvidesMediaParam;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kms.thrift.api.KmsMediaFaceOverlayImage;

/**
 * This {@link MediaParam} is used to configure the overlaying image position
 * and size. The position and size of the image that will appear in the video
 * feed, are relative to the size of the detected face, thus expressed here in
 * percentiles.
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 * @since 2.0.1
 * 
 */
@ProvidesMediaParam(type = SET_IMAGE_OVERLAY_PARAM_IMAGE_TYPE)
public final class FaceOverlayImageParam extends
		AbstractThriftSerializedMediaParam {

	private final KmsMediaFaceOverlayImage image = new KmsMediaFaceOverlayImage();

	/**
	 * The relative X coordinate of the overlaying image. Values can be either
	 * negative or positive. The former indicates a left displacement, while the
	 * latter indicates that the overlaying image will be displaced to the right
	 * of the overlaid image.
	 * 
	 * @return horizontal percentile displacement
	 */
	public float getOffsetXPercent() {
		return Double.valueOf(this.image.offsetXPercent).floatValue();
	}

	/**
	 * The relative Y coordinate of the overlaying image. Values can be either
	 * negative or positive. The former indicates lower displacement, while the
	 * latter indicates that the overlaying image will be displaced to the upper
	 * part of the overlaid image.
	 * 
	 * @return vertical percentile displacement
	 */
	public float getOffsetYPercent() {
		return Double.valueOf(this.image.offsetYPercent).floatValue();
	}

	/**
	 * The relative width of the overlaying image. A value > 1 indicates that
	 * the overlaying image will be bigger than overlaid one.
	 * 
	 * @return proportional width of the overlaying image
	 */
	public float getWidthPercent() {
		return Double.valueOf(this.image.widthPercent).floatValue();
	}

	/**
	 * The relative height of the image to be overlaid. A value > 1 indicates
	 * that the overlaying image will be bigger than overlaid one.
	 * 
	 * @return proportional height of the overlaying image
	 */
	public float getHeightPercent() {
		return Double.valueOf(this.image.heightPercent).floatValue();
	}

	/**
	 * The URI indicating the location of the image.
	 * 
	 * @return the URI
	 */
	public URI getUri() {
		try {
			return new URI(this.image.uri);
		} catch (URISyntaxException e) {
			// TODO we should not reach this section if the URI passed from the
			// media server is OK. This exception should never be thrown.
			throw new KurentoMediaFrameworkException(
					"The URI received from the Media Server does not conform the standard",
					e, 30000);
		}
	}

	/**
	 * Constructor to be used by the framework. If this constructor is used, the
	 * only way of filling the values from the overlaying image will be using a
	 * {@link KmsMediaFaceOverlayImage}
	 */
	public FaceOverlayImageParam() {
		super(SET_IMAGE_OVERLAY_PARAM_IMAGE_TYPE);
	}

	/**
	 * Constructor with parameters.
	 * 
	 * @param uri
	 *            The URI
	 * @param offsetXPercent
	 *            horizontal percentile displacement
	 * @param offsetYPercent
	 *            vertical percentile displacement
	 * @param widthPercent
	 *            proportional width of the overlaying image
	 * @param heightPercent
	 *            proportional height of the overlaying image
	 */
	public FaceOverlayImageParam(URI uri, float offsetXPercent,
			float offsetYPercent, float widthPercent, float heightPercent) {
		this();
		this.image.uri = uri.toString();
		this.image.offsetXPercent = offsetXPercent;
		this.image.offsetYPercent = offsetYPercent;
		this.image.widthPercent = widthPercent;
		this.image.heightPercent = heightPercent;
	}

	/**
	 * Constructor with parameters.
	 * 
	 * @param uri
	 *            The URI, according to the grammar specified in <a
	 *            href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
	 * @param offsetXPercent
	 *            horizontal percentile displacement
	 * @param offsetYPercent
	 *            vertical percentile displacement
	 * @param widthPercent
	 *            proportional width of the overlaying image
	 * @param heightPercent
	 *            proportional height of the overlaying image
	 * @throws URISyntaxException
	 *             if the URI passed as parameter does not comply with <a
	 *             href="http://www.ietf.org/rfc/rfc2396.txt">RFC&nbsp;2396</a>
	 */
	public FaceOverlayImageParam(String uri, float offsetXPercent,
			float offsetYPercent, float widthPercent, float heightPercent)
			throws URISyntaxException {
		this(new URI(uri), offsetXPercent, offsetYPercent, widthPercent,
				heightPercent);
	}

	@Override
	protected TProtocol serializeDataToThrift(TProtocol pr) {

		try {
			image.write(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
		return pr;
	}

	@Override
	protected void deserializeFromTProtocol(final TProtocol pr) {
		final KmsMediaFaceOverlayImage kmsParam = new KmsMediaFaceOverlayImage();
		try {
			kmsParam.read(pr);
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), 30000);
		}

		this.image.uri = kmsParam.uri.toString();
		this.image.offsetXPercent = kmsParam.offsetXPercent;
		this.image.offsetYPercent = kmsParam.offsetYPercent;
		this.image.widthPercent = kmsParam.widthPercent;
		this.image.heightPercent = kmsParam.heightPercent;
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

		FaceOverlayImageParam param = (FaceOverlayImageParam) obj;
		return this.image.equals(param.image);
	}

	@Override
	public int hashCode() {
		return this.image.hashCode();
	}

}
