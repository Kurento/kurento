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

#ifndef __KMS_BITRATE_FILTER_H__
#define __KMS_BITRATE_FILTER_H__

#include <gst/gst.h>
#include <gst/base/gstbasetransform.h>

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_BITRATE_FILTER \
  (kms_bitrate_filter_get_type())
#define KMS_BITRATE_FILTER(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_BITRATE_FILTER,KmsBitrateFilter))
#define KMS_BITRATE_FILTER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_BITRATE_FILTER,KmsBitrateFilterClass))
#define KMS_IS_BITRATE_FILTER(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_BITRATE_FILTER))
#define KMS_IS_BITRATE_FILTER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_BITRATE_FILTER))
#define KMS_BITRATE_FILTER_CAST(obj) ((KmsBitrateFilter*)(obj))

typedef struct _KmsBitrateFilter KmsBitrateFilter;
typedef struct _KmsBitrateFilterClass KmsBitrateFilterClass;
typedef struct _KmsBitrateFilterPrivate KmsBitrateFilterPrivate;

struct _KmsBitrateFilter
{
  GstBaseTransform parent;

  KmsBitrateFilterPrivate *priv;
};

struct _KmsBitrateFilterClass
{
  GstBaseTransformClass parent_class;
};

GType kms_bitrate_filter_get_type (void);

gboolean kms_bitrate_filter_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __KMS_BITRATE_FILTER_H__ */
