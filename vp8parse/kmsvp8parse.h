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

#ifndef _KMS_VP8_PARSE_H_
#define _KMS_VP8_PARSE_H_

#include <gst/base/gstbaseparse.h>

G_BEGIN_DECLS

#define KMS_TYPE_VP8_PARSE   (kms_vp8_parse_get_type())
#define KMS_VP8_PARSE(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_VP8_PARSE,KmsVp8Parse))
#define KMS_VP8_PARSE_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_VP8_PARSE,KmsVp8ParseClass))
#define KMS_IS_VP8_PARSE(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_VP8_PARSE))
#define KMS_IS_VP8_PARSE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_VP8_PARSE))

typedef struct _KmsVp8Parse KmsVp8Parse;
typedef struct _KmsVp8ParseClass KmsVp8ParseClass;

struct _KmsVp8Parse
{
  GstBaseParse base;
};

struct _KmsVp8ParseClass
{
  GstBaseParseClass base_class;
};

GType kms_vp8_parse_get_type (void);

gboolean kms_vp8_parse_plugin_init (GstPlugin * plugin);

G_END_DECLS

#endif  /* _KMS_VP8_PARSE_H_ */
