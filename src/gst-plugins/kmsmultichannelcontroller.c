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

#include <gio/gio.h>

#include "kmsmultichannelcontroller.h"
#include "kmsmccp.h"

GST_DEBUG_CATEGORY_STATIC (kms_multi_channel_controller_debug);
#define GST_CAT_DEFAULT kms_multi_channel_controller_debug

GType _kms_multi_channel_controller_type = 0;

#define KMS_MULTI_CHANNEL_CONTROLLER_LOCK(elem) \
  (g_rec_mutex_lock (&KMS_MULTI_CHANNEL_CONTROLLER ((elem))->rmutex))
#define KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK(elem) \
  (g_rec_mutex_unlock (&KMS_MULTI_CHANNEL_CONTROLLER ((elem))->rmutex))

struct _KmsMultiChannelController
{
  GstMiniObject obj;
  GRecMutex rmutex;

  MCLState state;
  MCLRole role;

  gchar *local_host;
  guint16 local_port;

  GCancellable *cancellable;

  GstTask *task;
  GRecMutex tmutex;
};

GST_DEFINE_MINI_OBJECT_TYPE (KmsMultiChannelController,
    kms_multi_channel_controller);

static void
_priv_kms_multi_channel_controller_initialize (void)
{
  _kms_multi_channel_controller_type = kms_multi_channel_controller_get_type ();

  GST_DEBUG_CATEGORY_INIT (kms_multi_channel_controller_debug,
      "multichannelcontroller", 0, "multi-channel controller protocol");
}

static void
_kms_multi_channel_controller_free (KmsMultiChannelController * mcc)
{
  g_return_if_fail (mcc != NULL);

  GST_DEBUG ("free");

  if (mcc->local_host != NULL)
    g_free (mcc->local_host);

  g_rec_mutex_clear (&mcc->rmutex);
  g_rec_mutex_clear (&mcc->tmutex);

  g_clear_object (&mcc->cancellable);

  g_slice_free1 (sizeof (KmsMultiChannelController), mcc);
}

KmsMultiChannelController *
kms_multi_channel_controller_new (const gchar * host, guint16 port)
{
  KmsMultiChannelController *mcc;

  mcc = g_slice_new0 (KmsMultiChannelController);

  gst_mini_object_init (GST_MINI_OBJECT_CAST (mcc), 0,
      _kms_multi_channel_controller_type, NULL, NULL,
      (GstMiniObjectFreeFunction) _kms_multi_channel_controller_free);

  g_rec_mutex_init (&mcc->rmutex);
  g_rec_mutex_init (&mcc->tmutex);
  mcc->cancellable = g_cancellable_new ();

  mcc->local_host = g_strdup (host);
  mcc->local_port = port;

  return KMS_MULTI_CHANNEL_CONTROLLER (mcc);
}

static void
kms_multi_channel_controller_thread (KmsMultiChannelController * mcc)
{
  /* TODO: Do stuff here */
}

gboolean
kms_multi_channel_controller_start (KmsMultiChannelController * mcc)
{
  GstTask *task;

  g_return_val_if_fail (mcc != NULL, FALSE);

  KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);

  if (mcc->task != NULL) {
    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
    return TRUE;
  }

  mcc->task = gst_task_new (
      (GstTaskFunction) kms_multi_channel_controller_thread, mcc, NULL);
  if (mcc->task == NULL) {
    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
    return FALSE;
  }

  gst_task_set_lock (mcc->task, &mcc->tmutex);

  if (gst_task_start (mcc->task)) {
    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
    return TRUE;
  }

  task = mcc->task;
  mcc->task = NULL;

  KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

  /* Task is not started */
  gst_task_join (task);
  gst_object_unref (GST_OBJECT (task));

  return FALSE;
}

void
kms_multi_channel_controller_stop (KmsMultiChannelController * mcc)
{
  GstTask *task;

  g_return_if_fail (mcc != NULL);

  KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);

  if ((task = mcc->task)) {
    mcc->task = NULL;

    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

    gst_task_stop (task);

    /* make sure it is not running */
    g_rec_mutex_lock (&mcc->tmutex);
    g_rec_mutex_unlock (&mcc->tmutex);

    /* now wait for the task to finish */
    gst_task_join (task);

    /* and free the task */
    gst_object_unref (GST_OBJECT (task));

  } else {
    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
  }
}

static void _priv_kms_multi_channel_controller_initialize (void)
    __attribute__ ((constructor));
