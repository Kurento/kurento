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

#ifndef __CLASSIFIER_H__
#define __CLASSIFIER_H__

#include <glib.h>

#include <opencv/cv.h>

G_BEGIN_DECLS

void classify_image (IplImage* img, CvSeq* facesList);

G_END_DECLS

#endif /* __CLASSIFIER_H__ */
