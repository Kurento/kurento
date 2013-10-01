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

#ifndef __KMS_AGNOSTIC_BIN2_H__
#define __KMS_AGNOSTIC_BIN2_H__

#include <gst/gst.h>

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
};

GType kms_agnostic_bin2_get_type (void);

gboolean kms_agnostic_bin2_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __KMS_AGNOSTIC_BIN2_H__ */
