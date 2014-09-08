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
#include "kmssctpconnection.h"

GST_DEBUG_CATEGORY_STATIC (kms_multi_channel_controller_debug);
#define GST_CAT_DEFAULT kms_multi_channel_controller_debug

GType _kms_multi_channel_controller_type = 0;

#define KMS_MULTI_CHANNEL_CONTROLLER_LOCK(elem) \
  (g_rec_mutex_lock (&KMS_MULTI_CHANNEL_CONTROLLER ((elem))->rmutex))
#define KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK(elem) \
  (g_rec_mutex_unlock (&KMS_MULTI_CHANNEL_CONTROLLER ((elem))->rmutex))

#define MCC_SCTP_DEFAULT_NUM_OSTREAMS 1
#define MCC_SCTP_DEFAULT_MAX_INSTREAMS 1

#define KMS_MCC_ERROR \
  g_quark_from_static_string("kms-multi-channel-controller-error-quark")

typedef enum
{
  KMS_MCC_INVALID_STATE
} KmsMCCError;

struct _KmsMultiChannelController
{
  GstMiniObject obj;
  GRecMutex rmutex;

  KmsSCTPConnection *mcl;

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

static const char *const state_str[] = {
  "CONNECTED",
  "PENDING",
  "ACTIVE",
  "IDLE"
};

#define STATE_STR(state) ({                         \
  const gchar *_state = NULL;                       \
  if (state >= 0 && state < G_N_ELEMENTS(state_str)) \
    _state = state_str[state];                      \
  _state;                                           \
})

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

  if (mcc->mcl != NULL) {
    kms_sctp_connection_close (mcc->mcl);
    kms_sctp_connection_unref (mcc->mcl);
  }

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

  mcc->state = MCL_IDLE;

  return KMS_MULTI_CHANNEL_CONTROLLER (mcc);
}

static void
kms_multi_channel_controller_accept (KmsMultiChannelController * mcc)
{
  KmsSCTPConnection *conn = NULL, *client = NULL;
  KmsSCTPResult result;
  GError *err = NULL;

  conn = kms_sctp_connection_new (mcc->local_host, mcc->local_port,
      mcc->cancellable, &err);

  if (conn == NULL)
    goto fail;

  if (!kms_sctp_connection_set_init_config (conn, MCC_SCTP_DEFAULT_NUM_OSTREAMS,
          MCC_SCTP_DEFAULT_MAX_INSTREAMS, 0, 0, &err))
    goto fail;

  if (kms_sctp_connection_bind (conn, mcc->cancellable, &err) != KMS_SCTP_OK)
    goto fail;

  /* wait on server socket for connections */
  result = kms_sctp_connection_accept (conn, mcc->cancellable, &client, &err);

  if (result != KMS_SCTP_OK)
    goto fail;

  /* Do not accept more connections */
  kms_sctp_connection_close (conn);
  kms_sctp_connection_unref (conn);

  KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);

  if (mcc->state != MCL_IDLE) {
    /* Control channel estblished at the time we were accepting connections */
    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
    kms_sctp_connection_close (client);
    kms_sctp_connection_unref (client);
    return;
  }

  mcc->state = MCL_CONNECTED;
  mcc->role = MCL_ACCEPTOR;
  mcc->mcl = client;
  KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

  return;

fail:
  if (err != NULL) {
    GST_ERROR_OBJECT (mcc, "%s", err->message);
    g_error_free (err);
  }

  if (conn != NULL)
    kms_sctp_connection_unref (conn);

  return;
}

static void
kms_multi_channel_controller_read_cmd (KmsMultiChannelController * mcc)
{
  /* TODO: Read multi-channel control protocol command */
}

static void
kms_multi_channel_controller_thread (KmsMultiChannelController * mcc)
{
  GST_DEBUG ("THREAD EXECUTED");
  KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);

  if (mcc->state == MCL_IDLE) {
    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
    kms_multi_channel_controller_accept (mcc);
  } else {
    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
    kms_multi_channel_controller_read_cmd (mcc);
  }
}

gboolean
kms_multi_channel_controller_connect (KmsMultiChannelController * mcc,
    gchar * host, guint16 port, GError ** err)
{
  KmsSCTPConnection *conn;
  KmsSCTPResult result;
  GError *e = NULL;

  KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);

  if (mcc->state != MCL_IDLE) {
    const gchar *state = STATE_STR (mcc->state);

    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
    g_set_error (err, KMS_MCC_ERROR, KMS_MCC_INVALID_STATE,
        "Operation is not allowed in %s", state);
    return FALSE;
  }

  KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

  conn = kms_sctp_connection_new (host, port, mcc->cancellable, &e);

  if (conn == NULL)
    goto fail;

  if (!kms_sctp_connection_set_init_config (conn, MCC_SCTP_DEFAULT_NUM_OSTREAMS,
          MCC_SCTP_DEFAULT_MAX_INSTREAMS, 0, 0, err)) {
    goto fail;
  }

  result = kms_sctp_connection_connect (conn, mcc->cancellable, err);
  if (result != KMS_SCTP_OK)
    goto fail;

  if (!kms_sctp_connection_set_event_subscribe (conn, KMS_SCTP_DATA_IO_EVENT,
          err))
    goto fail;

  KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);

  if (mcc->state != MCL_IDLE) {
    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
    goto fail;
  }

  mcc->mcl = conn;
  mcc->role = MCL_INITIATOR;
  mcc->state = MCL_CONNECTED;

  KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

  /* Stop accepting connections */
  g_cancellable_cancel (mcc->cancellable);

  return TRUE;

fail:
  {
    gboolean ret;

    if (conn != NULL) {
      kms_sctp_connection_close (conn);
      kms_sctp_connection_unref (conn);
    }

    KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);
    ret = mcc->state != MCL_IDLE;
    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

    if (e != NULL) {
      if (ret)
        g_error_free (e);
      else
        *err = e;
    }

    return ret;
  }
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

    g_cancellable_cancel (mcc->cancellable);

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
