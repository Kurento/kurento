/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

#ifndef __KMS_SDP_GROUP_MANAGER_H__
#define __KMS_SDP_GROUP_MANAGER_H__

#include <gst/gst.h>
#include <gst/sdp/gstsdpmessage.h>

#include "kmsisdpmediaextension.h"
#include "kmssdpbasegroup.h"

G_BEGIN_DECLS

#define KMS_TYPE_SDP_GROUP_MANAGER \
  (kms_sdp_group_manager_get_type())

#define KMS_SDP_GROUP_MANAGER(obj) (    \
  G_TYPE_CHECK_INSTANCE_CAST (          \
    (obj),                              \
    KMS_TYPE_SDP_GROUP_MANAGER,         \
    KmsSdpGroupManager                  \
  )                                     \
)
#define KMS_SDP_GROUP_MANAGER_CLASS(klass) (  \
  G_TYPE_CHECK_CLASS_CAST (                   \
    (klass),                                  \
    KMS_TYPE_SDP_GROUP_MANAGER,               \
    KmsSdpGroupManagerClass                   \
  )                                           \
)
#define KMS_IS_SDP_GROUP_MANAGER(obj) ( \
  G_TYPE_CHECK_INSTANCE_TYPE (          \
    (obj),                              \
    KMS_TYPE_SDP_GROUP_MANAGER          \
  )                                     \
)
#define KMS_IS_SDP_GROUP_MANAGER_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),KMS_TYPE_SDP_GROUP_MANAGER))
#define KMS_SDP_GROUP_MANAGER_GET_CLASS(obj) (  \
  G_TYPE_INSTANCE_GET_CLASS (                   \
    (obj),                                      \
    KMS_TYPE_SDP_GROUP_MANAGER,                 \
    KmsSdpGroupManagerClass                     \
  )                                             \
)

typedef struct _KmsSdpGroupManager KmsSdpGroupManager;
typedef struct _KmsSdpGroupManagerClass KmsSdpGroupManagerClass;
typedef struct _KmsSdpGroupManagerPrivate KmsSdpGroupManagerPrivate;

struct _KmsSdpGroupManager
{
  GObject parent;

  /*< private > */
  KmsSdpGroupManagerPrivate *priv;
};

struct _KmsSdpGroupManagerClass
{
  GObjectClass parent_class;

  /*< public >*/
  gint (*add_group) (KmsSdpGroupManager *obj, KmsSdpBaseGroup *group);
  void (*add_handler) (KmsSdpGroupManager *obj, KmsSdpHandler *handler);
  KmsSdpBaseGroup * (*get_group) (KmsSdpGroupManager *obj, KmsSdpHandler *handler);

  gboolean (*remove_handler) (KmsSdpGroupManager *obj, KmsSdpHandler *handler);
  gboolean (*add_handler_to_group) (KmsSdpGroupManager *obj, guint gid, guint hid);
  gboolean (*remove_handler_from_group) (KmsSdpGroupManager *obj, guint gid, guint hid);
  gboolean (*is_handler_valid_for_groups) (KmsSdpGroupManager *obj, const GstSDPMedia * media, const GstSDPMessage * offer, KmsSdpHandler *handler);
};

GType kms_sdp_group_manager_get_type ();

KmsSdpGroupManager * kms_sdp_group_manager_new ();

gint kms_sdp_group_manager_add_group (KmsSdpGroupManager *obj, KmsSdpBaseGroup *group);
void kms_sdp_group_manager_add_handler (KmsSdpGroupManager *obj, KmsSdpHandler *handler);
KmsSdpBaseGroup * kms_sdp_group_manager_get_group (KmsSdpGroupManager *obj, KmsSdpHandler *handler);
gboolean kms_sdp_group_manager_remove_handler (KmsSdpGroupManager *obj, KmsSdpHandler *handler);

gboolean kms_sdp_group_manager_add_handler_to_group (KmsSdpGroupManager *obj, guint gid, guint hid);
gboolean kms_sdp_group_manager_remove_handler_from_group (KmsSdpGroupManager *obj, guint gid, guint hid);

/* Return all groups managed. Groups provided are transfer full */
GList * kms_sdp_group_manager_get_groups (KmsSdpGroupManager *obj);
gboolean kms_sdp_group_manager_is_handler_valid_for_groups (KmsSdpGroupManager *obj, const GstSDPMedia * media, const GstSDPMessage * offer, KmsSdpHandler *handler);

G_END_DECLS

#endif /* __KMS_SDP_GROUP_MANAGER_H__ */
