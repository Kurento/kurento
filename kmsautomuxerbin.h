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
#ifndef __KMS_AUTOMUXER_BIN_H__
#define __KMS_AUTOMUXER_BIN_H__

#include <gst/gst.h>

G_BEGIN_DECLS
/* #defines don't like whitespacey bits */
#define KMS_TYPE_AUTOMUXER_BIN \
  (kms_automuxer_bin_get_type())
#define KMS_AUTOMUXER_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),KMS_TYPE_AUTOMUXER_BIN,KmsAutoMuxerBin))
#define KMS_AUTOMUXER_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),KMS_TYPE_AUTOMUXER_BIN,KmsAutoMuxerBinClass))
#define KMS_IS_AUTOMUXER_BIN(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),KMS_TYPE_AUTOMUXER_BIN))
#define KMS_IS_AUTOMUXER_BIN_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_AUTOMUXER_BIN))
#define KMS_AUTOMUXER_BIN_CAST(obj) ((KmsAutoMuxerBin*)(obj))
typedef struct _KmsAutoMuxerBin KmsAutoMuxerBin;
typedef struct _KmsAutoMuxerBinClass KmsAutoMuxerBinClass;
typedef struct _KmsAutoMuxerBinPrivate KmsAutoMuxerBinPrivate;

struct _KmsAutoMuxerBin
{
  GstBin parent;

  /*< private > */
  KmsAutoMuxerBinPrivate *priv;
};

struct _KmsAutoMuxerBinClass
{
  GstBinClass parent_class;
};

GType kms_automuxer_bin_get_type (void);

gboolean kms_automuxer_bin_plugin_init (GstPlugin * plugin);

G_END_DECLS
#endif /* __KMS_AUTOMUXER_BIN_H__ */
