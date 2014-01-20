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

import java.net.URI;

/**
 * FaceOverlayFilter interface. This type of {@code Filter} detects faces in a
 * video feed. The face is then overlaid with an image.
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.1
 */

public interface FaceOverlayFilter extends Filter {

	/**
	 * Sets the image to use as overlay on the detected faces.
	 * 
	 * @param uri
	 *            URI where the image is located.
	 * @param offsetXPercent
	 *            the offset applied to the image, from the X coordinate of the
	 *            detected's face upper right corner. A positive value indicates
	 *            right displacement, while a negative value moves the overlaid
	 *            image to the left. This offset is specified as a percentage of
	 *            the face width. For example, to cover the detected face with
	 *            the overlaid image, the parameter has to be 0.0. Values of 1.0
	 *            or -1.0 indicate that the image´s upper right corner will be
	 *            at the face´s X coord, +- the face´s width.
	 * @param offsetYPercent
	 *            the offset applied to the image, from the Y coordinate of the
	 *            detected's face upper right corner. A positive value indicates
	 *            up displacement, while a negative value moves the overlaid
	 *            image down. This offset is specified as a percentage of the
	 *            face width. For example, to cover the detected face with the
	 *            overlaid image, the parameter has to be 0.0. Values of 1.0 or
	 *            -1.0 indicate that the image´s upper right corner will be at
	 *            the face´s Y coord, +- the face´s width.
	 * @param widthPercent
	 *            proportional width of the overlaid image, relative to the
	 *            width of the detected face. A value of 1.0 implies that the
	 *            overlaid image will have the same width as the detected face.
	 *            Values greater than 1.0 are allowed, while negative values are
	 *            forbidden.
	 * @param heightPercent
	 *            proportional height of the overlaid image, relative to the
	 *            height of the detected face. A value of 1.0 implies that the
	 *            overlaid image will have the same height as the detected face.
	 *            Values greater than 1.0 are allowed, while negative values are
	 *            forbidden.
	 */
	void setOverlayedImage(URI uri, float offsetXPercent, float offsetYPercent,
			float widthPercent, float heightPercent);

	/**
	 * Sets the image to use as overlay on the detected faces.
	 * 
	 * @param uri
	 *            URI where the image is located.
	 * @param offsetXPercent
	 *            the offset applied to the image, from the X coordinate of the
	 *            detected's face upper right corner. A positive value indicates
	 *            right displacement, while a negative value moves the overlaid
	 *            image to the left. This offset is specified as a percentage of
	 *            the face width. For example, to cover the detected face with
	 *            the overlaid image, the parameter has to be 0.0. Values of 1.0
	 *            or -1.0 indicate that the image´s upper right corner will be
	 *            at the face´s X coord, +- the face´s width.
	 * @param offsetYPercent
	 *            the offset applied to the image, from the Y coordinate of the
	 *            detected's face upper right corner. A positive value indicates
	 *            up displacement, while a negative value moves the overlaid
	 *            image down. This offset is specified as a percentage of the
	 *            face width. For example, to cover the detected face with the
	 *            overlaid image, the parameter has to be 0.0. Values of 1.0 or
	 *            -1.0 indicate that the image´s upper right corner will be at
	 *            the face´s Y coord, +- the face´s width.
	 * @param widthPercent
	 *            proportional width of the overlaid image, relative to the
	 *            width of the detected face. A value of 1.0 implies that the
	 *            overlaid image will have the same width as the detected face.
	 *            Values greater than 1.0 are allowed, while negative values are
	 *            forbidden.
	 * @param heightPercent
	 *            proportional height of the overlaid image, relative to the
	 *            height of the detected face. A value of 1.0 implies that the
	 *            overlaid image will have the same height as the detected face.
	 *            Values greater than 1.0 are allowed, while negative values are
	 *            forbidden.
	 */
	void setOverlayedImage(String uri, float offsetXPercent,
			float offsetYPercent, float widthPercent, float heightPercent);

	/**
	 * Unsets the image to be shown over each detected face
	 */
	void unsetOverlayedImage();

	/**
	 * Unsets the image to be shown over each detected face
	 * 
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked if the command executed
	 *            correctly
	 */
	void unsetOverlayedImage(Continuation<Void> cont);

	/**
	 * Sets the image to use as overlay on the detected faces.
	 * 
	 * @param uri
	 *            URI where the image is located.
	 * @param offsetXPercent
	 *            the offset applied to the image, from the X coordinate of the
	 *            detected's face upper right corner. A positive value indicates
	 *            right displacement, while a negative value moves the overlaid
	 *            image to the left. This offset is specified as a percentage of
	 *            the face width. For example, to cover the detected face with
	 *            the overlaid image, the parameter has to be 0.0. Values of 1.0
	 *            or -1.0 indicate that the image´s upper right corner will be
	 *            at the face´s X coord, +- the face´s width.
	 * @param offsetYPercent
	 *            the offset applied to the image, from the Y coordinate of the
	 *            detected's face upper right corner. A positive value indicates
	 *            up displacement, while a negative value moves the overlaid
	 *            image down. This offset is specified as a percentage of the
	 *            face width. For example, to cover the detected face with the
	 *            overlaid image, the parameter has to be 0.0. Values of 1.0 or
	 *            -1.0 indicate that the image´s upper right corner will be at
	 *            the face´s Y coord, +- the face´s width.
	 * @param widthPercent
	 *            proportional width of the overlaid image, relative to the
	 *            width of the detected face. A value of 1.0 implies that the
	 *            overlaid image will have the same width as the detected face.
	 *            Values greater than 1.0 are allowed, while negative values are
	 *            forbidden.
	 * @param heightPercent
	 *            proportional height of the overlaid image, relative to the
	 *            height of the detected face. A value of 1.0 implies that the
	 *            overlaid image will have the same height as the detected face.
	 *            Values greater than 1.0 are allowed, while negative values are
	 *            forbidden.
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked if the command executed
	 *            correctly
	 */
	void setOverlayedImage(URI uri, float offsetXPercent, float offsetYPercent,
			float widthPercent, float heightPercent, Continuation<Void> cont);

	/**
	 * Sets the image to use as overlay on the detected faces.
	 * 
	 * @param uri
	 *            URI where the image is located.
	 * @param offsetXPercent
	 *            the offset applied to the image, from the X coordinate of the
	 *            detected's face upper right corner. A positive value indicates
	 *            right displacement, while a negative value moves the overlaid
	 *            image to the left. This offset is specified as a percentage of
	 *            the face width. For example, to cover the detected face with
	 *            the overlaid image, the parameter has to be 0.0. Values of 1.0
	 *            or -1.0 indicate that the image´s upper right corner will be
	 *            at the face´s X coord, +- the face´s width.
	 * @param offsetYPercent
	 *            the offset applied to the image, from the Y coordinate of the
	 *            detected's face upper right corner. A positive value indicates
	 *            up displacement, while a negative value moves the overlaid
	 *            image down. This offset is specified as a percentage of the
	 *            face width. For example, to cover the detected face with the
	 *            overlaid image, the parameter has to be 0.0. Values of 1.0 or
	 *            -1.0 indicate that the image´s upper right corner will be at
	 *            the face´s Y coord, +- the face´s width.
	 * @param widthPercent
	 *            proportional width of the overlaid image, relative to the
	 *            width of the detected face. A value of 1.0 implies that the
	 *            overlaid image will have the same width as the detected face.
	 *            Values greater than 1.0 are allowed, while negative values are
	 *            forbidden.
	 * @param heightPercent
	 *            proportional height of the overlaid image, relative to the
	 *            height of the detected face. A value of 1.0 implies that the
	 *            overlaid image will have the same height as the detected face.
	 *            Values greater than 1.0 are allowed, while negative values are
	 *            forbidden.
	 * @param cont
	 *            An asynchronous callback handler. The {@code onSuccess} method
	 *            from the handler will be invoked if the command executed
	 *            correctly
	 */
	void setOverlayedImage(String uri, float offsetXPercent,
			float offsetYPercent, float widthPercent, float heightPercent,
			Continuation<Void> cont);

	/**
	 * Builder for the {@link FaceOverlayFilter}.
	 * 
	 * @author Ivan Gracia (igracia@gsyc.es)
	 * @since 2.0.1
	 */
	public interface FaceOverlayFilterBuilder extends
			MediaObjectBuilder<FaceOverlayFilterBuilder, FaceOverlayFilter> {
		// No special method for the builder at this level
	}

}
