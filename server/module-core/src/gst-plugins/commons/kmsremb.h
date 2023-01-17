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

#ifndef __KMS_REMB_H__
#define __KMS_REMB_H__

#include "kmsutils.h" /* TODO: must be not needed */

G_BEGIN_DECLS

#define KMS_REMB_BASE(obj) ((KmsRembBase *)(obj))

#define KMS_REMB_BASE_LOCK(obj) \
  (g_rec_mutex_lock (&KMS_REMB_BASE(obj)->mutex))
#define KMS_REMB_BASE_UNLOCK(obj) \
  (g_rec_mutex_unlock (&KMS_REMB_BASE(obj)->mutex))

typedef struct _KmsRembBase KmsRembBase;

struct _KmsRembBase
{
  GObject *rtpsess;
  GRecMutex mutex;
  GHashTable *remb_stats;
  gulong signal_id;
};

/* KmsRembLocal begin */
typedef struct _KmsRembLocal KmsRembLocal;

struct _KmsRembLocal
{
  KmsRembBase base;

  GSList *remote_sessions; // List<KmsRlRemoteSession*>
  guint min_bw;
  guint max_bw;

  guint packets_recv_interval_top;
  gfloat exponential_factor;
  gint lineal_factor_min;
  gfloat lineal_factor_grade;
  gfloat decrement_factor;
  gfloat threshold_factor;
  gint up_losses;

  guint remb;
  guint remb_sent; // Used to limit logging at kms_remb_local_on_sending_rtcp()
  GstClockTime last_sent_time;
  gboolean probed;
  guint threshold;
  guint lineal_factor;
  guint max_br;
  guint avg_br;
  GstClockTime last_time;
  guint64 fraction_lost_record;
  RembEventManager *event_manager;
};

KmsRembLocal * kms_remb_local_create (GObject *rtpsess,
  guint min_bw, guint max_bw);
void kms_remb_local_destroy (KmsRembLocal *rl);
void kms_remb_local_add_remote_session (KmsRembLocal *rl, GObject *rtpsess, guint ssrc);
void kms_remb_local_set_params (KmsRembLocal *rl, GstStructure *params);
void kms_remb_local_get_params (KmsRembLocal *rl, GstStructure **params);
/* KmsRembLocal end */

/* KmsRembRemote begin */
typedef struct _KmsRembRemote KmsRembRemote;

struct _KmsRembRemote
{
  KmsRembBase base;

  guint local_ssrc;
  guint min_bw;
  guint max_bw;

  gint remb_on_connect;

  guint remb;
  gboolean probed;
  GstPad *pad_event;
};

KmsRembRemote * kms_remb_remote_create (GObject *rtpsess,
  guint local_ssrc, guint min_bw, guint max_bw, GstPad * pad);
void kms_remb_remote_destroy (KmsRembRemote *rm);
void kms_remb_remote_set_params (KmsRembRemote *rm, GstStructure *params);
void kms_remb_remote_get_params (KmsRembRemote *rm, GstStructure **params);
/* KmsRembRemote end */

G_END_DECLS
#endif /* __KMS_REMB_H__ */
