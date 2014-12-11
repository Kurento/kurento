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

#ifndef __KMS_REMB_H__
#define __KMS_REMB_H__

#include <commons/kmsutils.h> /* TODO: must be not needed */

G_BEGIN_DECLS

/* KmsRembLocal begin */
typedef struct _KmsRembLocal KmsRembLocal;

struct _KmsRembLocal
{
  GObject *rtpsess;
  guint remote_ssrc;
  guint max_bw;

  guint remb;
  gboolean probed;
  guint threshold;
  guint lineal_factor;
  guint max_br;
  guint avg_br;
  GstClockTime last_time;
  guint64 last_octets_received;
  RembEventManager *event_manager;
};

KmsRembLocal * kms_remb_local_create (GObject *rtpsess, guint remote_ssrc, guint max_bw);
void kms_remb_local_destroy (KmsRembLocal *rl);
/* KmsRembLocal end */

G_END_DECLS
#endif /* __KMS_REMB_H__ */
