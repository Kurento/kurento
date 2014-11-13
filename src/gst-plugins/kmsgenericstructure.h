
/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#ifndef __KMS_GENERIC_STRUCTURE_H__
#define __KMS_GENERIC_STRUCTURE_H__

#include <gst/gst.h>

G_BEGIN_DECLS

typedef struct _KmsGenericStructure KmsGenericStructure;

#define KMS_TYPE_GENERIC_STRUCTURE	(kms_generic_structure_get_type())
#define KMS_IS_GENERIC_STRUCTURE(obj)	(GST_IS_MINI_OBJECT_TYPE (obj, KMS_TYPE_GENERIC_STRUCTURE))
#define KMS_GENERIC_STRUCTURE_CAST(obj)	((KmsGenericStructure*)(obj))
#define KMS_GENERIC_STRUCTURE(obj)	(KMS_GENERIC_STRUCTURE_CAST(obj))

GType kms_generic_structure_get_type (void);

#ifdef _FOOL_GTK_DOC_
G_INLINE_FUNC KmsGenericStructure * kms_generic_structure_ref (KmsGenericStructure * b);
#endif

static inline KmsGenericStructure *
kms_generic_structure_ref (KmsGenericStructure * b)
{
  return KMS_GENERIC_STRUCTURE_CAST (gst_mini_object_ref (GST_MINI_OBJECT_CAST (b)));
}

#ifdef _FOOL_GTK_DOC_
G_INLINE_FUNC void kms_generic_structure_unref (KmsGenericStructure * b);
#endif

static inline void
kms_generic_structure_unref (KmsGenericStructure * b)
{
  gst_mini_object_unref (GST_MINI_OBJECT_CAST (b));
}

KmsGenericStructure * kms_generic_structure_new ();

#define kms_generic_structure_add(self, name, value) kms_generic_structure_add_full (self, name, value, NULL)

void kms_generic_structure_add_full (KmsGenericStructure *self,
  const gchar *name, gpointer value, GDestroyNotify notify);

gpointer kms_generic_structure_get (KmsGenericStructure *self, const gchar *name);
G_END_DECLS

#endif /* __KMS_GENERIC_STRUCTURE_H__ */
