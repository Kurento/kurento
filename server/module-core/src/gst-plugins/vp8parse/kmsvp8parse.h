/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
typedef struct _KmsVp8ParsePrivate KmsVp8ParsePrivate;

struct _KmsVp8Parse
{
  GstBaseParse base;
  KmsVp8ParsePrivate *priv;
};

struct _KmsVp8ParseClass
{
  GstBaseParseClass base_class;
};

GType kms_vp8_parse_get_type (void);

gboolean kms_vp8_parse_plugin_init (GstPlugin * plugin);

G_END_DECLS

#endif  /* _KMS_VP8_PARSE_H_ */
