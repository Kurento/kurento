/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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
#ifndef _KMS_SHOW_DATA_H_
#define _KMS_SHOW_DATA_H_

#include <gst/gst.h>
#include "commons/kmselement.h"

G_BEGIN_DECLS
#define KMS_TYPE_SHOW_DATA   (kms_show_data_get_type())
#define KMS_SHOW_DATA(obj)   (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_SHOW_DATA,KmsShowData))
#define KMS_SHOW_DATA_CLASS(klass)   (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_SHOW_DATA,KmsShowDataClass))
#define KMS_IS_SHOW_DATA(obj)   (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_SHOW_DATA))
#define KMS_IS_SHOW_DATA_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SHOW_DATA))
typedef struct _KmsShowData KmsShowData;
typedef struct _KmsShowDataClass KmsShowDataClass;
typedef struct _KmsShowDataPrivate KmsShowDataPrivate;

struct _KmsShowData
{
  KmsElement base;
  KmsShowDataPrivate *priv;
};

struct _KmsShowDataClass
{
  KmsElementClass parent_class;
};

GType kms_show_data_get_type (void);

gboolean kms_show_data_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* _KMS_SHOW_DATA_H_ */
