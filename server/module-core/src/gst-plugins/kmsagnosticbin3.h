/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#ifndef __KMS_AGNOSTIC_BIN3_H__
#define __KMS_AGNOSTIC_BIN3_H__

#include <gst/gst.h>

G_BEGIN_DECLS

#define KMS_TYPE_AGNOSTIC_BIN3 \
  (kms_agnostic_bin3_get_type())
#define KMS_AGNOSTIC_BIN3(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_AGNOSTIC_BIN3,KmsAgnosticBin3))
#define KMS_AGNOSTIC_BIN3_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_AGNOSTIC_BIN3,KmsAgnosticBin3Class))
#define KMS_IS_AGNOSTIC_BIN3(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_AGNOSTIC_BIN3))
#define KMS_IS_AGNOSTIC_BIN3_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_AGNOSTIC_BIN3))
#define KMS_AGNOSTIC_BIN3_CAST(obj) ((KmsAgnosticBin3*)(obj))

typedef struct _KmsAgnosticBin3 KmsAgnosticBin3;
typedef struct _KmsAgnosticBin3Class KmsAgnosticBin3Class;
typedef struct _KmsAgnosticBin3Private KmsAgnosticBin3Private;

struct _KmsAgnosticBin3
{
  GstBin parent;

  KmsAgnosticBin3Private *priv;
};

struct _KmsAgnosticBin3Class
{
  GstBinClass parent_class;

  /* signals */
  gboolean (*caps_signal) (KmsAgnosticBin3 * self, GstCaps *caps);
};

GType kms_agnostic_bin3_get_type (void);

gboolean kms_agnostic_bin3_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __KMS_AGNOSTIC_BIN3_H__ */
