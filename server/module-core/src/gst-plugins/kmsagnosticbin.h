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

#ifndef __KMS_AGNOSTIC_BIN2_H__
#define __KMS_AGNOSTIC_BIN2_H__

#include <gst/gst.h>

#include "commons/kmsmediatype.h"

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_AGNOSTIC_BIN2 \
  (kms_agnostic_bin2_get_type())
#define KMS_AGNOSTIC_BIN2(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_AGNOSTIC_BIN2,KmsAgnosticBin2))
#define KMS_AGNOSTIC_BIN2_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_AGNOSTIC_BIN2,KmsAgnosticBin2Class))
#define KMS_IS_AGNOSTIC_BIN2(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_AGNOSTIC_BIN2))
#define KMS_IS_AGNOSTIC_BIN2_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_AGNOSTIC_BIN2))
#define KMS_AGNOSTIC_BIN2_CAST(obj) ((KmsAgnosticBin2*)(obj))

typedef struct _KmsAgnosticBin2 KmsAgnosticBin2;
typedef struct _KmsAgnosticBin2Class KmsAgnosticBin2Class;
typedef struct _KmsAgnosticBin2Private KmsAgnosticBin2Private;

struct _KmsAgnosticBin2
{
  GstBin parent;

  KmsAgnosticBin2Private *priv;
};

struct _KmsAgnosticBin2Class
{
  GstBinClass parent_class;

  /* Signals */
  void (*media_transcoding) (GstBin *self, gboolean is_transcoding,
      KmsMediaType type);
};

GType kms_agnostic_bin2_get_type (void);

gboolean kms_agnostic_bin2_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __KMS_AGNOSTIC_BIN2_H__ */
