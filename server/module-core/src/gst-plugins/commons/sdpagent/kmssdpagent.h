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
#ifndef __KMS_SDP_AGENT_H__
#define __KMS_SDP_AGENT_H__

#include <gst/gst.h>
#include <gst/sdp/gstsdpmessage.h>
#include "kmssdpmediahandler.h"

G_BEGIN_DECLS

#define KMS_SDP_AGENT_ERROR \
  g_quark_from_static_string("kms-sdp-agent-error-quark")

typedef enum
{
  SDP_AGENT_INVALID_MEDIA,
  SDP_AGENT_INVALID_PARAMETER,
  SDP_AGENT_INVALID_PROTOCOL,
  SDP_AGENT_INVALID_STATE,
  SDP_AGENT_UNEXPECTED_ERROR
} SdpAgentError;

#define KMS_TYPE_SDP_AGENT \
  (kms_sdp_agent_get_type())

#define KMS_SDP_AGENT(obj) (            \
  G_TYPE_CHECK_INSTANCE_CAST (          \
    (obj),                              \
    KMS_TYPE_SDP_AGENT,                 \
    KmsSdpAgent                         \
  )                                     \
)
#define KMS_SDP_AGENT_CLASS(klass) (    \
  G_TYPE_CHECK_CLASS_CAST (             \
    (klass),                            \
    KMS_TYPE_SDP_AGENT,                 \
    KmsSdpAgentClass                    \
  )                                     \
)
#define KMS_IS_SDP_AGENT(obj) (         \
  G_TYPE_CHECK_INSTANCE_TYPE (          \
    (obj),                              \
    KMS_TYPE_SDP_AGENT                  \
  )                                     \
)
#define KMS_IS_SDP_AGENT_CLASS(klass) (    \
  G_TYPE_CHECK_CLASS_TYPE (                \
    (klass),                               \
    KMS_TYPE_SDP_AGENT                     \
  )                                        \
)
#define KMS_SDP_AGENT_GET_CLASS(obj) (    \
  G_TYPE_INSTANCE_GET_CLASS (             \
    (obj),                                \
    KMS_TYPE_SDP_AGENT,                   \
    KmsSdpAgentClass                      \
  )                                       \
)
typedef struct _KmsSdpAgent KmsSdpAgent;
typedef struct _KmsSdpAgentClass KmsSdpAgentClass;
typedef struct _KmsSdpAgentPrivate KmsSdpAgentPrivate;

struct _KmsSdpAgent
{
  GObject parent;

  /*< private > */
  KmsSdpAgentPrivate *priv;
};

struct _KmsSdpAgentClass
{
  GObjectClass parent_class;

  /* methods */
  gint (*add_proto_handler) (KmsSdpAgent * agent, const gchar *media, KmsSdpMediaHandler *handler, GError **error);
  gint (*get_handler_index) (KmsSdpAgent * agent, gint hid);
  GstSDPMessage *(*create_offer) (KmsSdpAgent * agent, GError **error);
  GstSDPMessage *(*create_answer) (KmsSdpAgent * agent, GError **error);
  gboolean (*cancel_offer) (KmsSdpAgent * agent, GError **error);
  gboolean (*set_local_description) (KmsSdpAgent * agent, GstSDPMessage * description, GError **error);
  gboolean (*set_remote_description) (KmsSdpAgent * agent, GstSDPMessage * description, GError **error);
};

GType kms_sdp_agent_get_type ();

KmsSdpAgent * kms_sdp_agent_new ();
gint kms_sdp_agent_add_proto_handler (KmsSdpAgent * agent, const gchar *media, KmsSdpMediaHandler *handler, GError **error);
gboolean kms_sdp_agent_remove_proto_handler (KmsSdpAgent * agent, gint hid, GError **error);
gint kms_sdp_agent_get_handler_index (KmsSdpAgent * agent, gint hid);
GstSDPMessage * kms_sdp_agent_create_answer (KmsSdpAgent * agent, GError **error);
gboolean kms_sdpagent_cancel_offer (KmsSdpAgent * agent, GError **error);
GstSDPMessage * kms_sdp_agent_create_offer (KmsSdpAgent * agent, GError **error);
gboolean kms_sdp_agent_set_local_description (KmsSdpAgent * agent, GstSDPMessage * description, GError **error);
gboolean kms_sdp_agent_set_remote_description (KmsSdpAgent * agent, GstSDPMessage * description, GError **error);
gint kms_sdp_agent_create_group (KmsSdpAgent * agent, GType group_type, GError **error, const char *optname1, ...);
gboolean kms_sdp_agent_group_add (KmsSdpAgent * agent, guint gid, guint hid, GError **error);
gboolean kms_sdp_agent_group_remove (KmsSdpAgent * agent, guint gid, guint hid, GError **error);

gint kms_sdp_agent_get_handler_group_id (KmsSdpAgent * agent, guint hid);
KmsSdpMediaHandler * kms_sdp_agent_get_handler_by_index (KmsSdpAgent * agent, guint index);

typedef struct {
    void (*on_media_offer) (KmsSdpAgent *agent, KmsSdpMediaHandler *handler,
                                GstSDPMedia *media, gpointer user_data);
    void (*on_media_answered) (KmsSdpAgent *agent,
                                   KmsSdpMediaHandler *handler,
                                   const GstSDPMedia *media,
                                   gboolean local_offerer,
                                   gpointer user_data);
    void (*on_media_answer) (KmsSdpAgent *agent, KmsSdpMediaHandler *handler,
                                GstSDPMedia *media, gpointer user_data);
    KmsSdpMediaHandler * (*on_handler_required) (KmsSdpAgent *agent,
                                                 const GstSDPMedia *media,
                                                 gpointer user_data);
} KmsSdpAgentCallbacks;

void kms_sdp_agent_set_callbacks (KmsSdpAgent * agent,
  KmsSdpAgentCallbacks * callbacks, gpointer user_data, GDestroyNotify destroy);

gboolean kms_sdp_media_handler_set_parent (KmsSdpMediaHandler *handler, KmsSdpAgent * parent, GError **error);

G_END_DECLS

#endif /* __KMS_SDP_AGENT_H__ */
