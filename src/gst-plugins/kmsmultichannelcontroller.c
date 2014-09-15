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
#include <arpa/inet.h>
#include <string.h>

#include "kmsmultichannelcontroller.h"
#include "kmssctpconnection.h"

GST_DEBUG_CATEGORY_STATIC (kms_multi_channel_controller_debug);
#define GST_CAT_DEFAULT kms_multi_channel_controller_debug

GType _kms_multi_channel_controller_type = 0;

#define KMS_MULTI_CHANNEL_CONTROLLER_LOCK(elem) \
  (g_rec_mutex_lock (&KMS_MULTI_CHANNEL_CONTROLLER ((elem))->rmutex))
#define KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK(elem) \
  (g_rec_mutex_unlock (&KMS_MULTI_CHANNEL_CONTROLLER ((elem))->rmutex))

#define KMS_MULTI_CHANNEL_CONTROLLER_SET_PENDING(elem) ({         \
  g_mutex_lock (&KMS_MULTI_CHANNEL_CONTROLLER ((elem))->mutex);   \
  KMS_MULTI_CHANNEL_CONTROLLER ((elem))->pending =TRUE;           \
  g_mutex_unlock (&KMS_MULTI_CHANNEL_CONTROLLER ((elem))->mutex); \
})

#define MCC_SCTP_DEFAULT_NUM_OSTREAMS 1
#define MCC_SCTP_DEFAULT_MAX_INSTREAMS 1

#define KMS_MCC_TIMEOUT 5       /*seconds */

#define KMS_MCC_ERROR \
  g_quark_from_static_string("kms-multi-channel-controller-error-quark")

typedef enum
{
  KMS_MCC_INVALID_STATE,
  KMS_MCC_REQUEST_TIMEOUT,
  KMS_MCC_BUSY,
  KMS_MCC_UNEXPECTED_ERROR
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

  GCond bound_cond;
  GMutex bound_mutex;
  gboolean bound;
  guint16 bound_port;

  GCancellable *cancellable;

  GstTask *task;
  GRecMutex tmutex;

  GCond cond;
  GMutex mutex;
  gboolean pending;

  gboolean waiting_rsp;
  KmsSCTPMessage msg_rsp;

  KmsCreateStreamFunction create_func;
  gpointer create_data;
  GDestroyNotify create_notify;
};

#define CONVERT(data, type) ({   \
  union _ConversionData          \
  {                              \
    guint8 *uint8;               \
    gpointer *buff;              \
  } _conv = { .uint8 = (data) }; \
  (* (type *) _conv.buff);       \
})

GST_DEFINE_MINI_OBJECT_TYPE (KmsMultiChannelController,
    kms_multi_channel_controller);

static const char *const state_str[] = {
  "CONNECTED",
  "PENDING",
  "ACTIVE",
  "IDLE"
};

static const char *const error_str[] = {
  NULL,
  "Invalid operation code",
  "Invalid parameter",
  "Unspecified error",
  "Request not supported"
};

#define ERROR_STR(code) ({                         \
  const gchar *_strt = NULL;                       \
  if (code > 0 && code < G_N_ELEMENTS(error_str))  \
    _strt = error_str[code];                       \
  _strt;                                           \
})

#define STATE_STR(state) ({                         \
  const gchar *_state = NULL;                       \
  if (state >= 0 && state < G_N_ELEMENTS(state_str)) \
    _state = state_str[state];                      \
  _state;                                           \
})

/* MCCP finite state machine functions */
static void
kms_multi_channel_controller_req_connected (KmsMultiChannelController * mcc,
    gchar * cmd, guint32 l);
static void kms_multi_channel_controller_req_pending (KmsMultiChannelController
    * mcc, gchar * cmd, guint32 l);
static void kms_multi_channel_controller_req_active (KmsMultiChannelController *
    mcc, gchar * cmd, guint32 l);

static void (*proc_req[]) (KmsMultiChannelController * mcc, gchar * cmd,
    guint32 l) = {
kms_multi_channel_controller_req_connected,
      kms_multi_channel_controller_req_pending,
      kms_multi_channel_controller_req_active};

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

  if (mcc->create_notify != NULL) {
    mcc->create_notify (mcc->create_data);
  }

  g_rec_mutex_clear (&mcc->rmutex);
  g_rec_mutex_clear (&mcc->tmutex);

  g_mutex_clear (&mcc->mutex);
  g_cond_clear (&mcc->cond);

  g_mutex_clear (&mcc->bound_mutex);
  g_cond_clear (&mcc->bound_cond);

  g_clear_object (&mcc->cancellable);

  g_slice_free1 (sizeof (KmsMultiChannelController), mcc);
}

static void
kms_multi_channel_controller_change_state (KmsMultiChannelController * mcc,
    MCLState new)
{
  if (mcc->state != new) {
    GST_INFO_OBJECT (mcc, "State change from %s to %s", STATE_STR (mcc->state),
        STATE_STR (new));
    mcc->state = new;
  }
}

static gboolean
kms_multi_channel_controller_wait_rsp (KmsMultiChannelController * mcc,
    GError ** err)
{
  gint64 end_time;
  gboolean ret;

  g_mutex_lock (&mcc->mutex);

  end_time = g_get_monotonic_time () + KMS_MCC_TIMEOUT * G_TIME_SPAN_SECOND;

  while (mcc->pending) {
    if (!g_cond_wait_until (&mcc->cond, &mcc->mutex, end_time)) {
      g_set_error (err, KMS_MCC_ERROR, KMS_MCC_REQUEST_TIMEOUT,
          "Response timed out");
      mcc->pending = FALSE;
      ret = FALSE;
      goto end;
    }
  }

  ret = TRUE;

  /* take message */

end:
  g_mutex_unlock (&mcc->mutex);

  return ret;
}

static void
kms_multi_channel_controller_wake_up (KmsMultiChannelController * mcc)
{
  g_mutex_lock (&mcc->mutex);

  mcc->pending = FALSE;

  g_cond_signal (&mcc->cond);
  g_mutex_unlock (&mcc->mutex);
}

static gboolean
kms_multi_channel_controller_send_message (KmsMultiChannelController * mcc,
    KmsSCTPMessage * msg, GError ** err)
{
  KmsSCTPResult result;

  KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);

  if (mcc->mcl == NULL) {
    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
    g_set_error (err, KMS_MCC_ERROR, KMS_MCC_UNEXPECTED_ERROR,
        "No control link established");
    return FALSE;
  }

  result =
      kms_sctp_connection_send (mcc->mcl, 0, 0, msg, mcc->cancellable, err);

  KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

  return result == KMS_SCTP_OK;
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

  g_mutex_init (&mcc->mutex);
  g_cond_init (&mcc->cond);

  g_mutex_init (&mcc->bound_mutex);
  g_cond_init (&mcc->bound_cond);

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

  g_mutex_lock (&mcc->bound_mutex);
  mcc->bound = TRUE;
  mcc->bound_port = kms_sctp_connection_get_bound_port (conn);
  g_cond_signal (&mcc->bound_cond);
  g_mutex_unlock (&mcc->bound_mutex);

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

  kms_multi_channel_controller_change_state (mcc, MCL_CONNECTED);
  mcc->role = MCL_ACCEPTOR;
  mcc->mcl = client;
  KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

  return;

fail:
  {
    KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);

    if (mcc->state == MCL_CONNECTED && mcc->role == MCL_INITIATOR &&
        g_cancellable_is_cancelled (mcc->cancellable)) {
      /* The accept operation was cancelled during the transition */
      /* from IDLE to CONNECTED. Restore the cancellable object. */
      g_cancellable_reset (mcc->cancellable);
      KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

      kms_multi_channel_controller_wake_up (mcc);
    } else {
      KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
    }

    if (err != NULL && !g_error_matches (err, G_IO_ERROR, G_IO_ERROR_CANCELLED)) {
      GST_ERROR_OBJECT (mcc, "%s", err->message);
    }

    g_clear_error (&err);

    if (conn != NULL)
      kms_sctp_connection_unref (conn);

    return;
  }
}

static gboolean
kms_multi_channel_controller_send_rsp (KmsMultiChannelController * mcc,
    guint8 oc, guint8 rc, guint16 chanid, gchar * buff, gsize len)
{
  KmsSCTPMessage msg = { 0 };
  GError *err = NULL;
  mccp_rsp *cmd;
  gboolean ret;

  cmd = g_malloc (sizeof (mccp_rsp) + len);
  cmd->op = oc;
  cmd->rc = rc;
  cmd->chanid = htons (chanid);

  if (buff && len > 0)
    memcpy (cmd->data, buff, len);

  msg.buf = (gchar *) cmd;
  msg.size = msg.used = sizeof (mccp_rsp) + len;

  if (!(ret = kms_multi_channel_controller_send_message (mcc, &msg, &err))) {
    GST_ERROR ("%s", err->message);
    g_error_free (err);
  }

  CLEAR_SCTP_MESSAGE (msg);

  return ret;
}

static gboolean
kms_multi_channel_controller_create_channel_req (KmsMultiChannelController *
    mcc, gchar * cmd, guint32 l)
{
  mccp_create_channel_req *req;
  guint16 port;
  gchar *data = NULL;
  gsize len = 0;
  guint16 chanid = 0;
  guint8 oc, rc;
  gboolean success = FALSE;
  StreamType type;

  oc = MCCP_CREATE_CHANNEL_RSP;

  if (l < sizeof (mccp_create_channel_req)) {
    rc = MCAP_UNSPECIFIED_ERROR;
    goto send_msg;
  }

  req = (mccp_create_channel_req *) cmd;

  switch (req->ct) {
    case MCCP_STREAM_TYPE_AUDIO:
      type = STREAM_TYPE_AUDIO;
      break;
    case MCCP_STREAM_TYPE_VIDEO:
      type = STREAM_TYPE_VIDEO;
      break;
    default:
      rc = MCCP_INVALID_PARAM_VALUE;
      goto send_msg;
  }

  if (mcc->create_func != NULL) {
    MCLState old;
    gint p;

    old = mcc->state;
    kms_multi_channel_controller_change_state (mcc, MCL_PENDING);

    /* Get port provided from callback request */
    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

    if ((p = mcc->create_func (type, chanid, mcc->create_data)) < 0) {
      KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);
      kms_multi_channel_controller_change_state (mcc, old);
      rc = MCAP_UNSPECIFIED_ERROR;
      goto send_msg;
    }

    KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);
    port = p;
  } else {
    rc = MCAP_REQUEST_NOT_SUPPORTED;
    goto send_msg;
  }

  rc = MCCP_SUCCESS;
  chanid = ntohs (req->chanid);

  data = (gchar *) & port;

  port = htons (port);
  len = sizeof (guint16);
  success = TRUE;

send_msg:
  kms_multi_channel_controller_send_rsp (mcc, oc, rc, chanid, data, len);

  return success;
}

static void
kms_multi_channel_controller_req_connected (KmsMultiChannelController * mcc,
    gchar * cmd, guint32 l)
{
  switch (cmd[0]) {
    case MCCP_CREATE_CHANNEL_REQ:
      if (kms_multi_channel_controller_create_channel_req (mcc, cmd, l))
        kms_multi_channel_controller_change_state (mcc, MCL_ACTIVE);
      break;
    default:

      break;
  }
}

static void
kms_multi_channel_controller_req_pending (KmsMultiChannelController * mcc,
    gchar * cmd, guint32 l)
{
  GST_DEBUG ("TODO: len %d", l);
}

static void
kms_multi_channel_controller_req_active (KmsMultiChannelController * mcc,
    gchar * cmd, guint32 l)
{
  switch (cmd[0]) {
    case MCCP_CREATE_CHANNEL_REQ:
      if (kms_multi_channel_controller_create_channel_req (mcc, cmd, l))
        kms_multi_channel_controller_change_state (mcc, MCL_ACTIVE);
      break;
    default:
      break;
  }
}

static void
kms_multi_channel_controller_proc_message (KmsMultiChannelController * mcc,
    KmsSCTPMessage * msg)
{
  if (msg->used <= 0) {
    GST_DEBUG ("Ignored request");
  }

  KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);

  if (mcc->waiting_rsp) {
    if (msg->buf[0] & 0x01) {
      /* Request arrived when when response is expected */
      if (mcc->role == MCL_INITIATOR) {
        /* ignore */
        KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
        return;
      }
      /* Acceptor should proccess request as normal */
      proc_req[mcc->state] (mcc, msg->buf, msg->used);
      KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
      return;
    }

    /* Process response */
    CLEAR_SCTP_MESSAGE (mcc->msg_rsp);
    mcc->msg_rsp.buf = msg->buf;
    mcc->msg_rsp.size = msg->size;
    mcc->msg_rsp.used = msg->used;
    msg->size = msg->used = 0;
    msg->buf = NULL;
    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

    /* wake up process */
    kms_multi_channel_controller_wake_up (mcc);
  } else if (msg->buf[0] & 0x01) {
    proc_req[mcc->state] (mcc, msg->buf, msg->used);
    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
  }
}

static void
kms_multi_channel_controller_thread (KmsMultiChannelController * mcc)
{
  KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);

  if (mcc->state == MCL_IDLE) {
    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
    kms_multi_channel_controller_accept (mcc);
  } else {
    KmsSCTPConnection *conn = NULL;
    KmsSCTPMessage msg = { 0 };
    KmsSCTPResult result;
    GError *err = NULL;

    if (mcc->mcl != NULL)
      conn = kms_sctp_connection_ref (mcc->mcl);

    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

    if (conn == NULL)
      return;

    INIT_SCTP_MESSAGE (msg, MCCP_MTU);

    result = kms_sctp_connection_receive (conn, &msg, mcc->cancellable, &err);

    switch (result) {
      case KMS_SCTP_OK:
        kms_multi_channel_controller_proc_message (mcc, &msg);
        break;
      case KMS_SCTP_EOF:
        KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);
        kms_multi_channel_controller_change_state (mcc, MCL_IDLE);
        KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
        break;
      default:
        GST_ERROR ("Error reading from SCTP socket (%d)", result);
    }

    if (err != NULL) {
      GST_ERROR_OBJECT (mcc, "%s", err->message);
      g_error_free (err);
    }

    CLEAR_SCTP_MESSAGE (msg);
    kms_sctp_connection_unref (conn);
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
  kms_multi_channel_controller_change_state (mcc, MCL_CONNECTED);

  KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

  KMS_MULTI_CHANNEL_CONTROLLER_SET_PENDING (mcc);

  /* Stop accepting connections */
  g_cancellable_cancel (mcc->cancellable);

  /* Wait to go to CONNECTED state */
  kms_multi_channel_controller_wait_rsp (mcc, NULL);

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

static int
kms_multi_channel_controller_create_media_stream_rsp (KmsMultiChannelController
    * mcc, StreamType type, guint16 chanid, GError ** err)
{
  mccp_rsp *rsp;
  guint16 id, data;
  int port = -1;

  KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);

  if (mcc->msg_rsp.used < (sizeof (mccp_rsp))) {
    g_set_error (err, KMS_MCC_ERROR, KMS_MCC_UNEXPECTED_ERROR,
        "Response error");
    goto end;
  }

  rsp = (mccp_rsp *) mcc->msg_rsp.buf;

  if (rsp->op != MCCP_CREATE_CHANNEL_RSP) {
    g_set_error (err, KMS_MCC_ERROR, KMS_MCC_UNEXPECTED_ERROR,
        "Invalid response");
    goto end;
  }

  if (rsp->rc != MCCP_SUCCESS) {
    g_set_error (err, KMS_MCC_ERROR, KMS_MCC_UNEXPECTED_ERROR, "Error %s",
        ERROR_STR (rsp->rc));
    goto end;
  }

  id = ntohs (rsp->chanid);

  if (id != chanid) {
    g_set_error (err, KMS_MCC_ERROR, KMS_MCC_UNEXPECTED_ERROR,
        "Protocol error");
    goto end;
  }

  if (mcc->msg_rsp.used < (sizeof (mccp_rsp) + sizeof (guint16))) {
    g_set_error (err, KMS_MCC_ERROR, KMS_MCC_UNEXPECTED_ERROR,
        "Port not provided");
    goto end;
  }

  /* create channel response has an unsigned int16 packet in it */
  /* so we make the proper conversion */
  data = CONVERT (rsp->data, guint16);
  port = ntohs (data);

end:
  KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

  return port;
}

int
kms_multi_channel_controller_create_media_stream (KmsMultiChannelController *
    mcc, StreamType type, guint16 chanid, GError ** err)
{
  mccp_create_channel_req *req;
  KmsSCTPMessage msg = { 0 };
  MCLState old;
  int port = -1;

  KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);

  if (mcc->waiting_rsp) {
    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
    g_set_error (err, KMS_MCC_ERROR, KMS_MCC_BUSY,
        "Other operation is taking place");
    return FALSE;
  }

  if (mcc->state != MCL_CONNECTED && mcc->state != MCL_ACTIVE) {
    const gchar *state = STATE_STR (mcc->state);

    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);
    g_set_error (err, KMS_MCC_ERROR, KMS_MCC_INVALID_STATE,
        "Operation is not allowed in %s", state);
    return FALSE;
  }

  mcc->waiting_rsp = TRUE;
  old = mcc->state;
  kms_multi_channel_controller_change_state (mcc, MCL_ACTIVE);

  KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

  req = g_new0 (mccp_create_channel_req, 1);
  req->op = MCCP_CREATE_CHANNEL_REQ;
  req->ct = (type == STREAM_TYPE_AUDIO) ? MCCP_STREAM_TYPE_AUDIO :
      MCCP_STREAM_TYPE_VIDEO;
  req->chanid = htons (chanid);

  msg.buf = (gchar *) req;
  msg.size = msg.used = sizeof (mccp_create_channel_req);

  KMS_MULTI_CHANNEL_CONTROLLER_SET_PENDING (mcc);

  if (!kms_multi_channel_controller_send_message (mcc, &msg, err)) {
    /* Restore preious state */
    KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);
    kms_multi_channel_controller_change_state (mcc, old);
    KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

    goto end;
  }

  if (!kms_multi_channel_controller_wait_rsp (mcc, err)) {
    goto end;
  }

  port = kms_multi_channel_controller_create_media_stream_rsp (mcc, type,
      chanid, err);

end:
  CLEAR_SCTP_MESSAGE (msg);

  KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);
  mcc->waiting_rsp = FALSE;
  KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

  return port;
}

void kms_multi_channel_controller_set_create_stream_callback
    (KmsMultiChannelController * mcc, KmsCreateStreamFunction func,
    gpointer user_data, GDestroyNotify notify)
{
  GDestroyNotify destroy;
  gpointer data;

  g_return_if_fail (mcc != NULL);

  KMS_MULTI_CHANNEL_CONTROLLER_LOCK (mcc);

  destroy = mcc->create_notify;
  data = mcc->create_data;

  mcc->create_notify = notify;
  mcc->create_data = user_data;
  mcc->create_func = func;

  KMS_MULTI_CHANNEL_CONTROLLER_UNLOCK (mcc);

  if (destroy != NULL) {
    destroy (data);
  }
}

int
kms_multi_channel_controller_get_bound_port (KmsMultiChannelController * mcc)
{
  gint64 end_time;
  gint port;

  g_return_val_if_fail (mcc != NULL, -1);

  g_mutex_lock (&mcc->bound_mutex);

  end_time = g_get_monotonic_time () + 2 * G_TIME_SPAN_SECOND;

  while (!mcc->bound) {
    if (!g_cond_wait_until (&mcc->bound_cond, &mcc->bound_mutex, end_time)) {
      /* Error */
      port = -1;
      goto end;
    }
  }

  port = mcc->bound_port;

end:
  g_cond_signal (&mcc->bound_cond);
  g_mutex_unlock (&mcc->bound_mutex);

  return port;
}

static void _priv_kms_multi_channel_controller_initialize (void)
    __attribute__ ((constructor));
