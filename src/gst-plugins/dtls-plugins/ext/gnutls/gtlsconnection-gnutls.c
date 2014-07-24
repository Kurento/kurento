/* GIO - GLib Input, Output and Streaming Library
 *
 * Copyright 2009 Red Hat, Inc
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 */

#include "config.h"
#include "glib.h"

#include <errno.h>
#include <gnutls/gnutls.h>
#include <gnutls/dtls.h>
#include <gnutls/x509.h>

#include "gtlsconnection-gnutls.h"
#include "gtlsbackend-gnutls.h"
#include "gtlscertificate-gnutls.h"
#include "gtlsinputstream-gnutls.h"
#include "gtlsoutputstream-gnutls.h"
#include "gtlsserverconnection-gnutls.h"

#ifdef HAVE_PKCS11
#include <p11-kit/pin.h>
#include "pkcs11/gpkcs11pin.h"
#endif

#include <glib/gi18n-lib.h>

#include "ext/gio/kmsgioenumtypes.h"

static ssize_t g_tls_connection_gnutls_push_func (gnutls_transport_ptr_t  transport_data,
						  const void             *buf,
						  size_t                  buflen);
static ssize_t g_tls_connection_gnutls_pull_func (gnutls_transport_ptr_t  transport_data,
						  void                   *buf,
						  size_t                  buflen);

static int     g_tls_connection_gnutls_pull_timeout_func (gnutls_transport_ptr_t transport_data,
							  unsigned int ms);


static void     g_tls_connection_gnutls_initable_iface_init (GInitableIface  *iface);
static gboolean g_tls_connection_gnutls_initable_init       (GInitable       *initable,
							     GCancellable    *cancellable,
							     GError         **error);

#ifdef HAVE_PKCS11
static P11KitPin*    on_pin_prompt_callback  (const char     *pinfile,
                                              P11KitUri      *pin_uri,
                                              const char     *pin_description,
                                              P11KitPinFlags  pin_flags,
                                              void           *callback_data);
#endif

static void g_tls_connection_gnutls_init_priorities (void);

static gboolean do_implicit_handshake (KmsGTlsConnectionGnutls  *gnutls,
				       gboolean               blocking,
				       GCancellable          *cancellable,
				       GError               **error);
static gboolean finish_handshake (KmsGTlsConnectionGnutls  *gnutls,
				  GTask                 *thread_task,
				  GError               **error);

G_DEFINE_ABSTRACT_TYPE_WITH_CODE (KmsGTlsConnectionGnutls, g_tls_connection_gnutls, G_TYPE_TLS_CONNECTION,
				  G_IMPLEMENT_INTERFACE (G_TYPE_INITABLE,
							 g_tls_connection_gnutls_initable_iface_init);
				  g_tls_connection_gnutls_init_priorities ();
				  );

  static gboolean g_gio_stream_is_datagram (GIOStream            *stream);
// static gboolean g_tls_connection_gnutls_is_datagram (GIOStream            *stream);

enum
{
  PROP_0,
  PROP_BASE_IO_STREAM,
  PROP_REQUIRE_CLOSE_NOTIFY,
  PROP_REHANDSHAKE_MODE,
  PROP_USE_SYSTEM_CERTDB,
  PROP_DATABASE,
  PROP_CERTIFICATE,
  PROP_INTERACTION,
  PROP_PEER_CERTIFICATE,
  PROP_PEER_CERTIFICATE_ERRORS,
  PROP_STATUS
};

struct _KmsGTlsConnectionGnutlsPrivate
{
  GIOStream *base_io_stream;
  GPollableInputStream *base_istream;
  GPollableOutputStream *base_ostream;

  gnutls_certificate_credentials_t creds;
  gnutls_session_t session;

  GTlsCertificate *certificate, *peer_certificate;
  GTlsCertificateFlags peer_certificate_errors;
  GTlsCertificate *peer_certificate_tmp;
  GTlsCertificateFlags peer_certificate_errors_tmp;

  gboolean require_close_notify;
  GTlsRehandshakeMode rehandshake_mode;
  gboolean is_system_certdb;
  GTlsDatabase *database;
  gboolean database_is_unset;

  /* need_handshake means the next claim_op() will get diverted into
   * an implicit handshake (unless it's an OP_HANDSHAKE or OP_CLOSE).
   * need_finish_handshake means the next claim_op() will get diverted
   * into finish_handshake() (unless it's an OP_CLOSE).
   *
   * handshaking is TRUE as soon as a handshake thread is queued. For
   * a sync handshake it becomes FALSE after finish_handshake()
   * completes in the calling thread, but for an async implicit
   * handshake, it becomes FALSE (and need_finish_handshake becomes
   * TRUE) at the end of the handshaking thread (and then the next
   * non-close op will call finish_handshake()). We can't just wait
   * for handshake_thread_completed() to run, because it's possible
   * that its main loop is being blocked by a synchronous op which is
   * waiting for handshaking to become FALSE...
   *
   * started_handshake indicates that the current handshake attempt
   * got at least as far as calling gnutls_handshake() (and so any
   * error should be copied to handshake_error and returned on all
   * future operations). ever_handshaked indicates that TLS has
   * been successfully negotiated at some point.
   */
  gboolean need_handshake, need_finish_handshake;
  gboolean started_handshake, handshaking, ever_handshaked;
  GTask *implicit_handshake;
  GError *handshake_error;
  GByteArray *app_data_buf;

  gboolean closing, closed;

  GInputStream *tls_istream;
  GOutputStream *tls_ostream;

  GTlsInteraction *interaction;
  gchar *interaction_id;

  GMutex        op_mutex;
  GCancellable *waiting_for_op;

  gboolean      reading;
  gboolean      read_blocking;
  GError       *read_error;
  GCancellable *read_cancellable;

  gboolean      writing;
  gboolean      write_blocking;
  GError       *write_error;
  GCancellable *write_cancellable;


#ifndef GNUTLS_E_PREMATURE_TERMINATION
  gboolean eof;
#endif
};

static gint unique_interaction_id = 0;

#ifdef KMS_DEBUG
static void tls_log_func(int level, const char *str)
{
    g_print ("GNUTLS_LOG |<%d>| %s", level, str);
}
#endif

static gpointer
gtls_gnutls_init (gpointer data)
{
#ifdef KMS_DEBUG
  gnutls_global_set_log_function(tls_log_func);
#endif
  gnutls_global_set_log_level(9);
  gnutls_global_init ();

  return NULL;
}

static GOnce gnutls_inited = G_ONCE_INIT;

static void
g_tls_connection_gnutls_init (KmsGTlsConnectionGnutls *gnutls)
{
  gint unique_id;

  g_once (&gnutls_inited, gtls_gnutls_init, NULL);
  gnutls->priv = G_TYPE_INSTANCE_GET_PRIVATE (gnutls, G_TYPE_KMS_TLS_CONNECTION_GNUTLS, KmsGTlsConnectionGnutlsPrivate);

  gnutls_certificate_allocate_credentials (&gnutls->priv->creds);
  gnutls_certificate_set_verify_flags (gnutls->priv->creds,
				       GNUTLS_VERIFY_ALLOW_X509_V1_CA_CRT);

  gnutls->priv->need_handshake = TRUE;

  gnutls->priv->database_is_unset = TRUE;
  gnutls->priv->is_system_certdb = TRUE;

  unique_id = g_atomic_int_add (&unique_interaction_id, 1);
  gnutls->priv->interaction_id = g_strdup_printf ("gtls:%d", unique_id);

#ifdef HAVE_PKCS11
  p11_kit_pin_register_callback (gnutls->priv->interaction_id,
                                 on_pin_prompt_callback, gnutls, NULL);
#endif

  gnutls->priv->waiting_for_op = g_cancellable_new ();
  g_cancellable_cancel (gnutls->priv->waiting_for_op);
  g_mutex_init (&gnutls->priv->op_mutex);
}

/* First field is "ssl3 only", second is "allow unsafe rehandshaking" */
static gnutls_priority_t priorities[2][2];

static void
g_tls_connection_gnutls_init_priorities (void)
{
  const gchar *base_priority;
  gchar *ssl3_priority, *unsafe_rehandshake_priority, *ssl3_unsafe_rehandshake_priority;

  base_priority = g_getenv ("G_TLS_GNUTLS_PRIORITY");
  if (!base_priority)
    base_priority = "NORMAL:%COMPAT";

  ssl3_priority = g_strdup_printf ("%s:!VERS-TLS1.2:!VERS-TLS1.1:!VERS-TLS1.0", base_priority);
  unsafe_rehandshake_priority = g_strdup_printf ("%s:%%UNSAFE_RENEGOTIATION", base_priority);
  ssl3_unsafe_rehandshake_priority = g_strdup_printf ("%s:!VERS-TLS1.2:!VERS-TLS1.1:!VERS-TLS1.0:%%UNSAFE_RENEGOTIATION", base_priority);

  gnutls_priority_init (&priorities[FALSE][FALSE], base_priority, NULL);
  gnutls_priority_init (&priorities[TRUE][FALSE], ssl3_priority, NULL);
  gnutls_priority_init (&priorities[FALSE][TRUE], unsafe_rehandshake_priority, NULL);
  gnutls_priority_init (&priorities[TRUE][TRUE], ssl3_unsafe_rehandshake_priority, NULL);

  g_free (ssl3_priority);
  g_free (unsafe_rehandshake_priority);
  g_free (ssl3_unsafe_rehandshake_priority);
}

static void
g_tls_connection_gnutls_set_handshake_priority (KmsGTlsConnectionGnutls *gnutls)
{
  gboolean use_ssl3, unsafe_rehandshake;

  if (G_IS_TLS_CLIENT_CONNECTION (gnutls))
    use_ssl3 = g_tls_client_connection_get_use_ssl3 (G_TLS_CLIENT_CONNECTION (gnutls));
  else
    use_ssl3 = FALSE;
  unsafe_rehandshake = (gnutls->priv->rehandshake_mode == G_TLS_REHANDSHAKE_UNSAFELY);
  gnutls_priority_set (gnutls->priv->session,
		       priorities[use_ssl3][unsafe_rehandshake]);
}

static gboolean
g_tls_connection_gnutls_initable_init (GInitable     *initable,
				       GCancellable  *cancellable,
				       GError       **error)
{
  KmsGTlsConnectionGnutls *gnutls = G_KMS_TLS_CONNECTION_GNUTLS (initable);
  gboolean client = G_IS_TLS_CLIENT_CONNECTION (gnutls);
  guint flags = client ? GNUTLS_CLIENT : GNUTLS_SERVER;
  int status;

  g_return_val_if_fail (gnutls->priv->base_istream != NULL &&
			gnutls->priv->base_ostream != NULL, FALSE);


  if (g_gio_stream_is_datagram (gnutls->priv->base_io_stream))
    flags |= GNUTLS_DATAGRAM;

  gnutls_init (&gnutls->priv->session, flags);

  status = gnutls_credentials_set (gnutls->priv->session,
				   GNUTLS_CRD_CERTIFICATE,
				   gnutls->priv->creds);
  if (status != 0)
    {
      g_set_error (error, G_TLS_ERROR, G_TLS_ERROR_MISC,
		   _("Could not create TLS connection: %s"),
		   gnutls_strerror (status));
      return FALSE;
    }

  /* Some servers (especially on embedded devices) use tiny keys that
   * gnutls will reject by default. We want it to accept them.
   */
  gnutls_dh_set_prime_bits (gnutls->priv->session, 256);

  gnutls_transport_set_push_function (gnutls->priv->session,
				      g_tls_connection_gnutls_push_func);
  gnutls_transport_set_pull_function (gnutls->priv->session,
				      g_tls_connection_gnutls_pull_func);
  gnutls_transport_set_pull_timeout_function (gnutls->priv->session,
				      g_tls_connection_gnutls_pull_timeout_func);
  gnutls_transport_set_ptr (gnutls->priv->session, gnutls);

  /* Don't enforce MTU */
  if (flags & GNUTLS_DATAGRAM)
    gnutls_dtls_set_mtu (gnutls->priv->session, 65535);

  gnutls->priv->tls_istream = g_tls_input_stream_gnutls_new (gnutls);
  gnutls->priv->tls_ostream = g_tls_output_stream_gnutls_new (gnutls);

  return TRUE;
}

static void
g_tls_connection_gnutls_finalize (GObject *object)
{
  KmsGTlsConnectionGnutls *gnutls = G_KMS_TLS_CONNECTION_GNUTLS (object);

  g_clear_object (&gnutls->priv->base_io_stream);

  g_clear_object (&gnutls->priv->tls_istream);
  g_clear_object (&gnutls->priv->tls_ostream);

  if (gnutls->priv->session)
    gnutls_deinit (gnutls->priv->session);
  if (gnutls->priv->creds)
    gnutls_certificate_free_credentials (gnutls->priv->creds);

  g_clear_object (&gnutls->priv->database);
  g_clear_object (&gnutls->priv->certificate);
  g_clear_object (&gnutls->priv->peer_certificate);
  g_clear_object (&gnutls->priv->peer_certificate_tmp);

  g_clear_pointer (&gnutls->priv->app_data_buf, g_byte_array_unref);

#ifdef HAVE_PKCS11
  p11_kit_pin_unregister_callback (gnutls->priv->interaction_id,
                                   on_pin_prompt_callback, gnutls);
#endif
  g_free (gnutls->priv->interaction_id);
  g_clear_object (&gnutls->priv->interaction);

  g_clear_error (&gnutls->priv->handshake_error);
  g_clear_error (&gnutls->priv->read_error);
  g_clear_error (&gnutls->priv->write_error);

  g_clear_object (&gnutls->priv->waiting_for_op);
  g_mutex_clear (&gnutls->priv->op_mutex);

  G_OBJECT_CLASS (g_tls_connection_gnutls_parent_class)->finalize (object);
}

static void
g_tls_connection_gnutls_get_property (GObject    *object,
				      guint       prop_id,
				      GValue     *value,
				      GParamSpec *pspec)
{
  KmsGTlsConnectionGnutls *gnutls = G_KMS_TLS_CONNECTION_GNUTLS (object);
#if 0 /* glib */
  GTlsBackend *backend;
#endif

  switch (prop_id)
    {
    case PROP_BASE_IO_STREAM:
      g_value_set_object (value, gnutls->priv->base_io_stream);
      break;

    case PROP_REQUIRE_CLOSE_NOTIFY:
      g_value_set_boolean (value, gnutls->priv->require_close_notify);
      break;

    case PROP_REHANDSHAKE_MODE:
      g_value_set_enum (value, gnutls->priv->rehandshake_mode);
      break;

    case PROP_USE_SYSTEM_CERTDB:
      g_value_set_boolean (value, gnutls->priv->is_system_certdb);
      break;

    case PROP_DATABASE:
      if (gnutls->priv->database_is_unset)
        {

#if 0 /* glib */
          backend = g_tls_backend_get_default ();
#else
          gnutls->priv->database =  g_tls_backend_gnutls_get_default_database_static ();
#endif
          gnutls->priv->database_is_unset = FALSE;
        }
      g_value_set_object (value, gnutls->priv->database);
      break;

    case PROP_CERTIFICATE:
      g_value_set_object (value, gnutls->priv->certificate);
      break;

    case PROP_INTERACTION:
      g_value_set_object (value, gnutls->priv->interaction);
      break;

    case PROP_PEER_CERTIFICATE:
      g_value_set_object (value, gnutls->priv->peer_certificate);
      break;

    case PROP_PEER_CERTIFICATE_ERRORS:
      g_value_set_flags (value, gnutls->priv->peer_certificate_errors);
      break;
    case PROP_STATUS:
      if (gnutls->priv->closing || gnutls->priv->closed)
	{
	  g_value_set_enum (value, G_TLS_STATUS_CLOSED);
	}
      else if (gnutls->priv->started_handshake ||
	       gnutls->priv->handshaking ||
	       gnutls->priv->need_handshake ||
	       gnutls->priv->need_finish_handshake)
	{
	  if (gnutls->priv->ever_handshaked)
	    g_value_set_enum (value, G_TLS_STATUS_REHANDSHAKING);
	  else
	    g_value_set_enum (value, G_TLS_STATUS_HANDSHAKING);
	}
      else if (gnutls->priv->handshake_error)
	{
	  g_value_set_enum (value, G_TLS_STATUS_ERROR);
	}
      else
	{
	  g_value_set_enum (value, G_TLS_STATUS_CONNECTED);
	}
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
    }
}

static void
g_tls_connection_gnutls_set_property (GObject      *object,
				      guint         prop_id,
				      const GValue *value,
				      GParamSpec   *pspec)
{
  KmsGTlsConnectionGnutls *gnutls = G_KMS_TLS_CONNECTION_GNUTLS (object);
  GInputStream *istream;
  GOutputStream *ostream;
  gboolean system_certdb;
#if 0 /* glib */
  GTlsBackend *backend;
#endif
  switch (prop_id)
    {
    case PROP_BASE_IO_STREAM:
      if (gnutls->priv->base_io_stream)
	{
	  g_object_unref (gnutls->priv->base_io_stream);
	  gnutls->priv->base_istream = NULL;
	  gnutls->priv->base_ostream = NULL;
	}
      gnutls->priv->base_io_stream = g_value_dup_object (value);
      if (!gnutls->priv->base_io_stream)
	return;

      istream = g_io_stream_get_input_stream (gnutls->priv->base_io_stream);
      ostream = g_io_stream_get_output_stream (gnutls->priv->base_io_stream);

      if (G_IS_POLLABLE_INPUT_STREAM (istream) &&
	  g_pollable_input_stream_can_poll (G_POLLABLE_INPUT_STREAM (istream)))
	gnutls->priv->base_istream = G_POLLABLE_INPUT_STREAM (istream);
      if (G_IS_POLLABLE_OUTPUT_STREAM (ostream) &&
	  g_pollable_output_stream_can_poll (G_POLLABLE_OUTPUT_STREAM (ostream)))
	gnutls->priv->base_ostream = G_POLLABLE_OUTPUT_STREAM (ostream);
      break;

    case PROP_REQUIRE_CLOSE_NOTIFY:
      gnutls->priv->require_close_notify = g_value_get_boolean (value);
      break;

    case PROP_REHANDSHAKE_MODE:
      gnutls->priv->rehandshake_mode = g_value_get_enum (value);
      break;

    case PROP_USE_SYSTEM_CERTDB:
      system_certdb = g_value_get_boolean (value);
      if (system_certdb != gnutls->priv->is_system_certdb)
        {
          g_clear_object (&gnutls->priv->database);
          if (system_certdb)
            {
#if 0 /* glib */
              backend = g_tls_backend_get_default ();
#else
              gnutls->priv->database = g_tls_backend_gnutls_get_default_database_static ();
#endif
            }
          gnutls->priv->is_system_certdb = system_certdb;
          gnutls->priv->database_is_unset = FALSE;
        }
      break;

    case PROP_DATABASE:
      g_clear_object (&gnutls->priv->database);
      gnutls->priv->database = g_value_dup_object (value);
      gnutls->priv->is_system_certdb = FALSE;
      gnutls->priv->database_is_unset = FALSE;
      break;

    case PROP_CERTIFICATE:
      if (gnutls->priv->certificate)
	g_object_unref (gnutls->priv->certificate);
      gnutls->priv->certificate = g_value_dup_object (value);
      break;

    case PROP_INTERACTION:
      g_clear_object (&gnutls->priv->interaction);
      gnutls->priv->interaction = g_value_dup_object (value);
      break;

    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
    }
}

gnutls_certificate_credentials_t
g_tls_connection_gnutls_get_credentials (KmsGTlsConnectionGnutls *gnutls)
{
  return gnutls->priv->creds;
}

gnutls_session_t
g_tls_connection_gnutls_get_session (KmsGTlsConnectionGnutls *gnutls)
{
  return gnutls->priv->session;
}

void
g_tls_connection_gnutls_get_certificate (KmsGTlsConnectionGnutls *gnutls,
                                         gnutls_retr2_st      *st)
{
  GTlsCertificate *cert;

  cert = g_tls_connection_get_certificate (G_TLS_CONNECTION (gnutls));

  st->cert_type = GNUTLS_CRT_X509;
  st->ncerts = 0;

  if (cert)
      g_tls_certificate_gnutls_copy (G_KMS_TLS_CERTIFICATE_GNUTLS (cert),
                                     gnutls->priv->interaction_id, st);
}

typedef enum {
  G_KMS_TLS_CONNECTION_GNUTLS_OP_HANDSHAKE,
  G_KMS_TLS_CONNECTION_GNUTLS_OP_READ,
  G_KMS_TLS_CONNECTION_GNUTLS_OP_WRITE,
  G_KMS_TLS_CONNECTION_GNUTLS_OP_CLOSE,
} KmsGTlsConnectionGnutlsOp;

static gboolean
claim_op (KmsGTlsConnectionGnutls    *gnutls,
	  KmsGTlsConnectionGnutlsOp   op,
	  gboolean                 blocking,
	  GCancellable            *cancellable,
	  GError                 **error)
{
 try_again:
  if (g_cancellable_set_error_if_cancelled (cancellable, error))
    return FALSE;

  g_mutex_lock (&gnutls->priv->op_mutex);

  if (gnutls->priv->closing || gnutls->priv->closed)
    {
      g_set_error_literal (error, G_IO_ERROR, G_IO_ERROR_CLOSED,
			   _("Connection is closed"));
      g_mutex_unlock (&gnutls->priv->op_mutex);
      return FALSE;
    }

  if (gnutls->priv->handshake_error && op != G_KMS_TLS_CONNECTION_GNUTLS_OP_CLOSE)
    {
      if (error)
	*error = g_error_copy (gnutls->priv->handshake_error);
      g_mutex_unlock (&gnutls->priv->op_mutex);
      return FALSE;
    }

  if (op != G_KMS_TLS_CONNECTION_GNUTLS_OP_HANDSHAKE &&
      op != G_KMS_TLS_CONNECTION_GNUTLS_OP_CLOSE)
    {
      if (gnutls->priv->need_handshake)
	{
	  gnutls->priv->need_handshake = FALSE;
	  gnutls->priv->handshaking = TRUE;
	  if (!do_implicit_handshake (gnutls, blocking, cancellable, error))
	    {
	      g_mutex_unlock (&gnutls->priv->op_mutex);
	      return FALSE;
	    }
	}

      if (gnutls->priv->need_finish_handshake &&
	  gnutls->priv->implicit_handshake)
	{
	  GError *my_error = NULL;
	  gboolean success;

	  gnutls->priv->need_finish_handshake = FALSE;

	  g_mutex_unlock (&gnutls->priv->op_mutex);
	  success = finish_handshake (gnutls, gnutls->priv->implicit_handshake, &my_error);
	  g_clear_object (&gnutls->priv->implicit_handshake);
	  g_mutex_lock (&gnutls->priv->op_mutex);

	  if (!success || g_cancellable_set_error_if_cancelled (cancellable, &my_error))
	    {
	      g_propagate_error (error, my_error);
	      g_mutex_unlock (&gnutls->priv->op_mutex);
	      return FALSE;
	    }
	}
    }

  if ((op != G_KMS_TLS_CONNECTION_GNUTLS_OP_WRITE && gnutls->priv->reading) ||
      (op != G_KMS_TLS_CONNECTION_GNUTLS_OP_READ && gnutls->priv->writing) ||
      (op != G_KMS_TLS_CONNECTION_GNUTLS_OP_HANDSHAKE && gnutls->priv->handshaking))
    {
      GPollFD fds[2];
      int nfds;

      g_cancellable_reset (gnutls->priv->waiting_for_op);

      g_mutex_unlock (&gnutls->priv->op_mutex);

      if (!blocking)
	{
	  g_set_error_literal (error, G_IO_ERROR, G_IO_ERROR_WOULD_BLOCK,
			       _("Operation would block"));
	  return FALSE;
	}

      g_cancellable_make_pollfd (gnutls->priv->waiting_for_op, &fds[0]);
      if (g_cancellable_make_pollfd (cancellable, &fds[1]))
	nfds = 2;
      else
	nfds = 1;
      g_poll (fds, nfds, -1);
      g_cancellable_release_fd (cancellable);

      goto try_again;
    }

  if (op == G_KMS_TLS_CONNECTION_GNUTLS_OP_HANDSHAKE)
    {
      gnutls->priv->handshaking = TRUE;
      gnutls->priv->need_handshake = FALSE;
    }
  if (op == G_KMS_TLS_CONNECTION_GNUTLS_OP_CLOSE)
    gnutls->priv->closing = TRUE;

  if (op != G_KMS_TLS_CONNECTION_GNUTLS_OP_WRITE)
    gnutls->priv->reading = TRUE;
  if (op != G_KMS_TLS_CONNECTION_GNUTLS_OP_READ)
    gnutls->priv->writing = TRUE;

  g_mutex_unlock (&gnutls->priv->op_mutex);
  return TRUE;
}

static void
yield_op (KmsGTlsConnectionGnutls   *gnutls,
	  KmsGTlsConnectionGnutlsOp  op)
{
  g_mutex_lock (&gnutls->priv->op_mutex);

  if (op == G_KMS_TLS_CONNECTION_GNUTLS_OP_HANDSHAKE)
    gnutls->priv->handshaking = FALSE;
  if (op == G_KMS_TLS_CONNECTION_GNUTLS_OP_CLOSE)
    gnutls->priv->closing = FALSE;

  if (op != G_KMS_TLS_CONNECTION_GNUTLS_OP_WRITE)
    gnutls->priv->reading = FALSE;
  if (op != G_KMS_TLS_CONNECTION_GNUTLS_OP_READ)
    gnutls->priv->writing = FALSE;

  g_cancellable_cancel (gnutls->priv->waiting_for_op);
  g_mutex_unlock (&gnutls->priv->op_mutex);
}

static void
begin_gnutls_io (KmsGTlsConnectionGnutls  *gnutls,
		 GIOCondition           direction,
		 gboolean               blocking,
		 GCancellable          *cancellable)
{
  g_assert (direction & (G_IO_IN | G_IO_OUT));

  if (direction & G_IO_IN)
    {
      gnutls->priv->read_blocking = blocking;
      gnutls->priv->read_cancellable = cancellable;
      g_clear_error (&gnutls->priv->read_error);
    }

  if (direction & G_IO_OUT)
    {
      gnutls->priv->write_blocking = blocking;
      gnutls->priv->write_cancellable = cancellable;
      g_clear_error (&gnutls->priv->write_error);
    }
}

static int
end_gnutls_io (KmsGTlsConnectionGnutls  *gnutls,
	       GIOCondition           direction,
	       int                    status,
	       const char            *errmsg,
	       GError               **error)
{
  GError *my_error = NULL;

  g_assert (direction & (G_IO_IN | G_IO_OUT));
  g_assert (!error || !*error);

  if (status == GNUTLS_E_AGAIN ||
      status == GNUTLS_E_WARNING_ALERT_RECEIVED)
    return GNUTLS_E_AGAIN;

  if (direction & G_IO_IN)
    {
      gnutls->priv->read_cancellable = NULL;
      if (status < 0)
	{
	  my_error = gnutls->priv->read_error;
	  gnutls->priv->read_error = NULL;
	}
      else
	g_clear_error (&gnutls->priv->read_error);
    }
  if (direction & G_IO_OUT)
    {
      gnutls->priv->write_cancellable = NULL;
      if (status < 0 && !my_error)
	{
	  my_error = gnutls->priv->write_error;
	  gnutls->priv->write_error = NULL;
	}
      else
	g_clear_error (&gnutls->priv->write_error);
    }

  if (status >= 0)
    return status;

  if (gnutls->priv->handshaking && !gnutls->priv->ever_handshaked)
    {
      if (g_error_matches (my_error, G_IO_ERROR, G_IO_ERROR_FAILED) ||
#if GLIB_CHECK_VERSION (2, 35, 3)
	  g_error_matches (my_error, G_IO_ERROR, G_IO_ERROR_BROKEN_PIPE) ||
#endif
	  status == GNUTLS_E_UNEXPECTED_PACKET_LENGTH ||
	  status == GNUTLS_E_FATAL_ALERT_RECEIVED ||
	  status == GNUTLS_E_DECRYPTION_FAILED ||
	  status == GNUTLS_E_UNSUPPORTED_VERSION_PACKET)
	{
	  g_clear_error (&my_error);
	  g_set_error_literal (error, G_TLS_ERROR, G_TLS_ERROR_NOT_TLS,
			       _("Peer failed to perform TLS handshake"));
	  return GNUTLS_E_PULL_ERROR;
	}
    }

  if (my_error)
    {
      if (!g_error_matches (my_error, G_IO_ERROR, G_IO_ERROR_WOULD_BLOCK))
	G_KMS_TLS_CONNECTION_GNUTLS_GET_CLASS (gnutls)->failed (gnutls);
      g_propagate_error (error, my_error);
      return status;
    }
  else if (status == GNUTLS_E_REHANDSHAKE)
    {
      if (gnutls->priv->rehandshake_mode == G_TLS_REHANDSHAKE_NEVER)
	{
	  g_set_error_literal (error, G_TLS_ERROR, G_TLS_ERROR_MISC,
			       _("Peer requested illegal TLS rehandshake"));
	  return GNUTLS_E_PULL_ERROR;
	}

      g_mutex_lock (&gnutls->priv->op_mutex);
      if (!gnutls->priv->handshaking)
	gnutls->priv->need_handshake = TRUE;
      g_mutex_unlock (&gnutls->priv->op_mutex);
      return status;
    }
  else if (
#ifdef GNUTLS_E_PREMATURE_TERMINATION
	   status == GNUTLS_E_PREMATURE_TERMINATION
#else
	   status == GNUTLS_E_UNEXPECTED_PACKET_LENGTH && gnutls->priv->eof
#endif
	   )
    {
      if (gnutls->priv->handshaking && !gnutls->priv->ever_handshaked)
	{
	  g_set_error_literal (error, G_TLS_ERROR, G_TLS_ERROR_NOT_TLS,
			       _("Peer failed to perform TLS handshake"));
	  return GNUTLS_E_PULL_ERROR;
	}
      else if (gnutls->priv->require_close_notify)
	{
	  g_set_error_literal (error, G_TLS_ERROR, G_TLS_ERROR_EOF,
			       _("TLS connection closed unexpectedly"));
	  G_KMS_TLS_CONNECTION_GNUTLS_GET_CLASS (gnutls)->failed (gnutls);
	  return status;
	}
      else
	return 0;
    }
  else if (status == GNUTLS_E_NO_CERTIFICATE_FOUND)
    {
      g_set_error_literal (error, G_TLS_ERROR, G_TLS_ERROR_CERTIFICATE_REQUIRED,
                           _("TLS connection peer did not send a certificate"));
      return status;
    }

  if (error)
    {
      g_set_error (error, G_TLS_ERROR, G_TLS_ERROR_MISC,
                   errmsg, gnutls_strerror (status));
    }
  return status;
}

#define BEGIN_GNUTLS_IO(gnutls, direction, blocking, cancellable)	\
  begin_gnutls_io (gnutls, direction, blocking, cancellable);		\
  do {

#define END_GNUTLS_IO(gnutls, direction, ret, errmsg, err)		\
  } while ((ret = end_gnutls_io (gnutls, direction, ret, errmsg, err)) == GNUTLS_E_AGAIN);

gboolean
g_tls_connection_gnutls_check (KmsGTlsConnectionGnutls  *gnutls,
			       GIOCondition           condition)
{
  /* Racy, but worst case is that we just get WOULD_BLOCK back */
  if (gnutls->priv->need_finish_handshake)
    return TRUE;

  /* If a handshake or close is in progress, then tls_istream and
   * tls_ostream are blocked, regardless of the base stream status.
   */
  if (gnutls->priv->handshaking || gnutls->priv->closing)
    return FALSE;

  if (condition & G_IO_IN)
    return g_pollable_input_stream_is_readable (gnutls->priv->base_istream);
  else
    return g_pollable_output_stream_is_writable (gnutls->priv->base_ostream);
}

typedef struct {
  GSource               source;

  KmsGTlsConnectionGnutls *gnutls;
  GObject              *stream;

  GSource              *child_source;
  GIOCondition          condition;

  gboolean              io_waiting;
  gboolean              op_waiting;
} KmsGTlsConnectionGnutlsSource;

static gboolean
gnutls_source_prepare (GSource *source,
		       gint    *timeout)
{
  *timeout = -1;
  return FALSE;
}

static gboolean
gnutls_source_check (GSource *source)
{
  return FALSE;
}

static void
gnutls_source_sync (KmsGTlsConnectionGnutlsSource *gnutls_source)
{
  KmsGTlsConnectionGnutls *gnutls = gnutls_source->gnutls;
  gboolean io_waiting, op_waiting;

  g_mutex_lock (&gnutls->priv->op_mutex);
  if (((gnutls_source->condition & G_IO_IN) && gnutls->priv->reading) ||
      ((gnutls_source->condition & G_IO_OUT) && gnutls->priv->writing) ||
      (gnutls->priv->handshaking && !gnutls->priv->need_finish_handshake))
    op_waiting = TRUE;
  else
    op_waiting = FALSE;

  if (!op_waiting && !gnutls->priv->need_handshake &&
      !gnutls->priv->need_finish_handshake)
    io_waiting = TRUE;
  else
    io_waiting = FALSE;
  g_mutex_unlock (&gnutls->priv->op_mutex);

  if (op_waiting == gnutls_source->op_waiting &&
      io_waiting == gnutls_source->io_waiting)
    return;
  gnutls_source->op_waiting = op_waiting;
  gnutls_source->io_waiting = io_waiting;

  if (gnutls_source->child_source)
    {
      g_source_remove_child_source ((GSource *)gnutls_source,
				    gnutls_source->child_source);
      g_source_unref (gnutls_source->child_source);
    }

  if (op_waiting)
    gnutls_source->child_source = g_cancellable_source_new (gnutls->priv->waiting_for_op);
  else if (io_waiting && G_IS_POLLABLE_INPUT_STREAM (gnutls_source->stream))
    gnutls_source->child_source = g_pollable_input_stream_create_source (gnutls->priv->base_istream, NULL);
  else if (io_waiting && G_IS_POLLABLE_OUTPUT_STREAM (gnutls_source->stream))
    gnutls_source->child_source = g_pollable_output_stream_create_source (gnutls->priv->base_ostream, NULL);
  else
    gnutls_source->child_source = g_timeout_source_new (0);

  g_source_set_dummy_callback (gnutls_source->child_source);
  g_source_add_child_source ((GSource *)gnutls_source, gnutls_source->child_source);
}

static gboolean
gnutls_source_dispatch (GSource     *source,
			GSourceFunc  callback,
			gpointer     user_data)
{
  GPollableSourceFunc func = (GPollableSourceFunc)callback;
  KmsGTlsConnectionGnutlsSource *gnutls_source = (KmsGTlsConnectionGnutlsSource *)source;
  gboolean ret;

  ret = (*func) (gnutls_source->stream, user_data);
  if (ret)
    gnutls_source_sync (gnutls_source);

  return ret;
}

static void
gnutls_source_finalize (GSource *source)
{
  KmsGTlsConnectionGnutlsSource *gnutls_source = (KmsGTlsConnectionGnutlsSource *)source;

  g_object_unref (gnutls_source->gnutls);
  g_source_unref (gnutls_source->child_source);
}

static gboolean
g_tls_connection_gnutls_source_closure_callback (GObject  *stream,
						 gpointer  data)
{
  GClosure *closure = data;

  GValue param = { 0, };
  GValue result_value = { 0, };
  gboolean result;

  g_value_init (&result_value, G_TYPE_BOOLEAN);

  g_value_init (&param, G_TYPE_OBJECT);
  g_value_set_object (&param, stream);

  g_closure_invoke (closure, &result_value, 1, &param, NULL);

  result = g_value_get_boolean (&result_value);
  g_value_unset (&result_value);
  g_value_unset (&param);

  return result;
}

static GSourceFuncs gnutls_source_funcs =
{
  gnutls_source_prepare,
  gnutls_source_check,
  gnutls_source_dispatch,
  gnutls_source_finalize,
  (GSourceFunc)g_tls_connection_gnutls_source_closure_callback,
  (GSourceDummyMarshal)g_cclosure_marshal_generic
};

GSource *
g_tls_connection_gnutls_create_source (KmsGTlsConnectionGnutls  *gnutls,
				       GIOCondition           condition,
				       GCancellable          *cancellable)
{
  GSource *source, *cancellable_source;
  KmsGTlsConnectionGnutlsSource *gnutls_source;

  source = g_source_new (&gnutls_source_funcs, sizeof (KmsGTlsConnectionGnutlsSource));
  g_source_set_name (source, "KmsGTlsConnectionGnutlsSource");
  gnutls_source = (KmsGTlsConnectionGnutlsSource *)source;
  gnutls_source->gnutls = g_object_ref (gnutls);
  gnutls_source->condition = condition;
  if (condition & G_IO_IN)
    gnutls_source->stream = G_OBJECT (gnutls->priv->tls_istream);
  else if (condition & G_IO_OUT)
    gnutls_source->stream = G_OBJECT (gnutls->priv->tls_ostream);

  gnutls_source->op_waiting = (gboolean) -1;
  gnutls_source->io_waiting = (gboolean) -1;
  gnutls_source_sync (gnutls_source);

  if (cancellable)
    {
      cancellable_source = g_cancellable_source_new (cancellable);
      g_source_set_dummy_callback (cancellable_source);
      g_source_add_child_source (source, cancellable_source);
      g_source_unref (cancellable_source);
    }

  return source;
}

static void
set_gnutls_error (KmsGTlsConnectionGnutls *gnutls,
		  GError               *error)
{
  /* We set EINTR rather than EAGAIN for G_IO_ERROR_WOULD_BLOCK so
   * that GNUTLS_E_AGAIN only gets returned for gnutls-internal
   * reasons, not for actual socket EAGAINs (and we have access
   * to @error at the higher levels, so we can distinguish them
   * that way later).
   */

  if (g_error_matches (error, G_IO_ERROR, G_IO_ERROR_CANCELLED))
    gnutls_transport_set_errno (gnutls->priv->session, EINTR);
  else if (g_error_matches (error, G_IO_ERROR, G_IO_ERROR_WOULD_BLOCK))
    gnutls_transport_set_errno (gnutls->priv->session, EINTR);
  else if (g_error_matches (error, G_IO_ERROR, G_IO_ERROR_INVALID_DATA)) //G_IO_ERROR_MESSAGE_TOO_LARGE))
    gnutls_transport_set_errno (gnutls->priv->session, EMSGSIZE);
  else
    gnutls_transport_set_errno (gnutls->priv->session, EIO);
}

static ssize_t
g_tls_connection_gnutls_pull_func (gnutls_transport_ptr_t  transport_data,
				   void                   *buf,
				   size_t                  buflen)
{
  KmsGTlsConnectionGnutls *gnutls = transport_data;
  ssize_t ret;

  /* If gnutls->priv->read_error is non-%NULL when we're called, it means
   * that an error previously occurred, but gnutls decided not to
   * propagate it. So it's correct for us to just clear it. (Usually
   * this means it ignored an EAGAIN after a short read, and now
   * we'll return EAGAIN again, which it will obey this time.)
   */
  g_clear_error (&gnutls->priv->read_error);

  ret = g_pollable_stream_read (G_INPUT_STREAM (gnutls->priv->base_istream),
				buf, buflen,
				gnutls->priv->read_blocking,
				gnutls->priv->read_cancellable,
				&gnutls->priv->read_error);

  if (ret < 0)
    set_gnutls_error (gnutls, gnutls->priv->read_error);
#ifndef GNUTLS_E_PREMATURE_TERMINATION
  else if (ret == 0)
    gnutls->priv->eof = TRUE;
#endif

  return ret;
}

static ssize_t
g_tls_connection_gnutls_push_func (gnutls_transport_ptr_t  transport_data,
				   const void             *buf,
				   size_t                  buflen)
{
  KmsGTlsConnectionGnutls *gnutls = transport_data;
  ssize_t ret;

  /* See comment in pull_func. */
  g_clear_error (&gnutls->priv->write_error);
  ret = g_pollable_stream_write (G_OUTPUT_STREAM (gnutls->priv->base_ostream),
				 buf, buflen,
				 gnutls->priv->write_blocking,
				 gnutls->priv->write_cancellable,
				 &gnutls->priv->write_error);

  if (ret < 0)
    set_gnutls_error (gnutls, gnutls->priv->write_error);

  return ret;
}

static gboolean
source_loop_quit (GPollableInputStream *istream, gpointer user_data)
{
  GMainLoop *loop = user_data;

  g_assert (G_IS_POLLABLE_INPUT_STREAM (istream));

  g_main_loop_quit (loop);

  return TRUE;
}

static int
g_tls_connection_gnutls_pull_timeout_func (gnutls_transport_ptr_t transport_data,
					   unsigned int ms)
{
  KmsGTlsConnectionGnutls *gnutls = transport_data;
  GMainContext *ctx = g_main_context_new ();
  GMainLoop *loop = g_main_loop_new (ctx, FALSE);
  GSource *timeout_source;
  GSource *read_source;

  timeout_source = g_timeout_source_new (ms);
  g_source_set_callback (timeout_source, (GSourceFunc) g_main_loop_quit, loop,
			 NULL);
  g_source_attach (timeout_source, ctx);

  read_source = g_pollable_input_stream_create_source (G_POLLABLE_INPUT_STREAM (gnutls->priv->base_istream),
						       gnutls->priv->read_cancellable);
  g_source_set_callback (read_source, (GSourceFunc) source_loop_quit, loop,
			 NULL);
  g_source_attach (read_source, ctx);

  g_main_loop_run (loop);

  g_source_destroy (timeout_source);
  g_source_destroy (read_source);

  g_main_loop_unref (loop);
  g_main_context_unref (ctx);
  g_source_unref (timeout_source);
  g_source_unref (read_source);

  if (g_pollable_input_stream_is_readable (G_POLLABLE_INPUT_STREAM (gnutls->priv->base_istream)))
    return 1;
  else
    return 0;
}


static GTlsCertificate *
get_peer_certificate_from_session (KmsGTlsConnectionGnutls *gnutls)
{
  GTlsCertificate *chain, *cert;
  const gnutls_datum_t *certs;
  unsigned int num_certs;
  int i;

  certs = gnutls_certificate_get_peers (gnutls->priv->session, &num_certs);
  if (!certs || !num_certs)
    return NULL;

  chain = NULL;
  for (i = num_certs - 1; i >= 0; i--)
    {
      cert = g_tls_certificate_gnutls_new (&certs[i], chain);
      if (chain)
	g_object_unref (chain);
      chain = cert;
    }

  return chain;
}

static GTlsCertificateFlags
verify_peer_certificate (KmsGTlsConnectionGnutls *gnutls,
			 GTlsCertificate      *peer_certificate)
{
  GTlsConnection *conn = G_TLS_CONNECTION (gnutls);
  GSocketConnectable *peer_identity;
  GTlsDatabase *database;
  GTlsCertificateFlags errors;
  gboolean is_client;

  is_client = G_IS_TLS_CLIENT_CONNECTION (gnutls);
  if (is_client)
    peer_identity = g_tls_client_connection_get_server_identity (G_TLS_CLIENT_CONNECTION (gnutls));
  else
    peer_identity = NULL;

  errors = 0;

  database = g_tls_connection_get_database (conn);
  if (database == NULL)
    {
      errors |= G_TLS_CERTIFICATE_UNKNOWN_CA;
      errors |= g_tls_certificate_verify (peer_certificate, peer_identity, NULL);
    }
  else
    {
      GError *error = NULL;

      errors |= g_tls_database_verify_chain (database, peer_certificate,
					     is_client ?
					     G_TLS_DATABASE_PURPOSE_AUTHENTICATE_SERVER :
					     G_TLS_DATABASE_PURPOSE_AUTHENTICATE_CLIENT,
					     peer_identity,
					     g_tls_connection_get_interaction (conn),
					     G_TLS_DATABASE_VERIFY_NONE,
					     NULL, &error);
      if (error)
	{
	  g_warning ("failure verifying certificate chain: %s",
		     error->message);
	  g_assert (errors != 0);
	  g_clear_error (&error);
	}
    }

  return errors;
}

#include <execinfo.h>

static void
handshake_thread (GTask        *task,
		  gpointer      object,
		  gpointer      task_data,
		  GCancellable *cancellable)
{
  KmsGTlsConnectionGnutls *gnutls = object;
  gboolean is_client;
  GError *error = NULL;
  int ret;

  gnutls->priv->started_handshake = FALSE;
  g_object_notify (G_OBJECT (gnutls), "status");

  if (!claim_op (gnutls, G_KMS_TLS_CONNECTION_GNUTLS_OP_HANDSHAKE,
		 TRUE, cancellable, &error))
    {
      g_task_return_error (task, error);
      return;
    }

  g_clear_error (&gnutls->priv->handshake_error);

  is_client = G_IS_TLS_CLIENT_CONNECTION (gnutls);

  if (!is_client && gnutls->priv->ever_handshaked &&
      !gnutls->priv->implicit_handshake)
    {
      BEGIN_GNUTLS_IO (gnutls, G_IO_IN | G_IO_OUT, TRUE, cancellable);
      ret = gnutls_rehandshake (gnutls->priv->session);
      END_GNUTLS_IO (gnutls, G_IO_IN | G_IO_OUT, ret,
		     _("Error performing TLS handshake: %s"), &error);

      if (error)
	{
	  g_task_return_error (task, error);
	  return;
	}
    }

  gnutls->priv->started_handshake = TRUE;
  g_clear_object (&gnutls->priv->peer_certificate);
  gnutls->priv->peer_certificate_errors = 0;
  g_tls_connection_gnutls_set_handshake_priority (gnutls);
  BEGIN_GNUTLS_IO (gnutls, G_IO_IN | G_IO_OUT, TRUE, cancellable);
  ret = gnutls_handshake (gnutls->priv->session);

  if (ret == GNUTLS_E_GOT_APPLICATION_DATA)
    {
      guint8 buf[1024];
      /* Got app data while waiting for rehandshake; buffer it and try again */
      ret = gnutls_record_recv (gnutls->priv->session, buf, sizeof (buf));
      if (ret > -1)
	{
	  if (!gnutls->priv->app_data_buf)
	    gnutls->priv->app_data_buf = g_byte_array_new ();
	  g_byte_array_append (gnutls->priv->app_data_buf, buf, ret);
	  ret = GNUTLS_E_AGAIN;
	}
    }
  END_GNUTLS_IO (gnutls, G_IO_IN | G_IO_OUT, ret,
		 _("Error performing TLS handshake: %s"), &error);
  if (ret == 0 && gnutls_certificate_type_get (gnutls->priv->session) == GNUTLS_CRT_X509)
    {
      gnutls->priv->peer_certificate_tmp = get_peer_certificate_from_session (gnutls);
      if (gnutls->priv->peer_certificate_tmp)
	gnutls->priv->peer_certificate_errors_tmp = verify_peer_certificate (gnutls, gnutls->priv->peer_certificate_tmp);
      else if (G_IS_TLS_CLIENT_CONNECTION (gnutls))
	{
	  g_set_error_literal (&error, G_TLS_ERROR, G_TLS_ERROR_BAD_CERTIFICATE,
			       _("Server did not return a valid TLS certificate"));
	}
    }
  G_KMS_TLS_CONNECTION_GNUTLS_GET_CLASS (gnutls)->finish_handshake (gnutls, &error);

  if (error)
    {
      g_task_return_error (task, error);
    }
  else
    {
      gnutls->priv->ever_handshaked = TRUE;
      g_task_return_boolean (task, TRUE);
    }
}

static gboolean
accept_peer_certificate (KmsGTlsConnectionGnutls *gnutls,
			 GTlsCertificate      *peer_certificate,
			 GTlsCertificateFlags  peer_certificate_errors)
{
  gboolean accepted;

  if (G_IS_TLS_CLIENT_CONNECTION (gnutls))
    {
      GTlsCertificateFlags validation_flags =
	g_tls_client_connection_get_validation_flags (G_TLS_CLIENT_CONNECTION (gnutls));

      if ((peer_certificate_errors & validation_flags) == 0)
	accepted = TRUE;
      else
	{
	  accepted = g_tls_connection_emit_accept_certificate (G_TLS_CONNECTION (gnutls),
							       peer_certificate,
							       peer_certificate_errors);
	}
    }
  else
    {
      accepted = g_tls_connection_emit_accept_certificate (G_TLS_CONNECTION (gnutls),
							   peer_certificate,
							   peer_certificate_errors);
    }

  return accepted;
}

static void
begin_handshake (KmsGTlsConnectionGnutls *gnutls)
{
  G_KMS_TLS_CONNECTION_GNUTLS_GET_CLASS (gnutls)->begin_handshake (gnutls);
}

static gboolean
finish_handshake (KmsGTlsConnectionGnutls  *gnutls,
		  GTask                 *task,
		  GError               **error)
{
  GTlsCertificate *peer_certificate;
  GTlsCertificateFlags peer_certificate_errors;

  g_assert (error != NULL);

  peer_certificate = gnutls->priv->peer_certificate_tmp;
  gnutls->priv->peer_certificate_tmp = NULL;
  peer_certificate_errors = gnutls->priv->peer_certificate_errors_tmp;
  gnutls->priv->peer_certificate_errors_tmp = 0;

  if (g_task_propagate_boolean (task, error) && peer_certificate)
    {
      if (!accept_peer_certificate (gnutls, peer_certificate,
				    peer_certificate_errors))
	{
	  g_set_error_literal (error, G_TLS_ERROR, G_TLS_ERROR_BAD_CERTIFICATE,
			       _("Unacceptable TLS certificate"));
	}

      gnutls->priv->peer_certificate = peer_certificate;
      gnutls->priv->peer_certificate_errors = peer_certificate_errors;
      g_object_notify (G_OBJECT (gnutls), "peer-certificate");
      g_object_notify (G_OBJECT (gnutls), "peer-certificate-errors");
    }

  if (*error && gnutls->priv->started_handshake)
    gnutls->priv->handshake_error = g_error_copy (*error);

  g_object_notify (G_OBJECT (gnutls), "status");

  return (*error == NULL);
}

static gboolean
g_tls_connection_gnutls_handshake (GTlsConnection   *conn,
				   GCancellable     *cancellable,
				   GError          **error)
{
  KmsGTlsConnectionGnutls *gnutls = G_KMS_TLS_CONNECTION_GNUTLS (conn);
  GTask *task;
  gboolean success;
  GError *my_error = NULL;

  task = g_task_new (conn, cancellable, NULL, NULL);
  begin_handshake (gnutls);
  g_task_run_in_thread_sync (task, handshake_thread);
  success = finish_handshake (gnutls, task, &my_error);
  g_object_unref (task);

  yield_op (gnutls, G_KMS_TLS_CONNECTION_GNUTLS_OP_HANDSHAKE);

  if (my_error)
    g_propagate_error (error, my_error);
  return success;
}

/* In the async version we use two GTasks; one to run handshake_thread() and
 * then call handshake_thread_completed(), and a second to call the caller's
 * original callback after we call finish_handshake().
 */

static void
handshake_thread_completed (GObject      *object,
			    GAsyncResult *result,
			    gpointer      user_data)
{
  GTask *caller_task = user_data;
  KmsGTlsConnectionGnutls *gnutls = g_task_get_source_object (caller_task);
  GError *error = NULL;
  gboolean need_finish_handshake, success;

  g_mutex_lock (&gnutls->priv->op_mutex);
  if (gnutls->priv->need_finish_handshake)
    {
      need_finish_handshake = TRUE;
      gnutls->priv->need_finish_handshake = FALSE;
    }
  else
    need_finish_handshake = FALSE;
  g_mutex_unlock (&gnutls->priv->op_mutex);

  if (need_finish_handshake)
    {
      success = finish_handshake (gnutls, G_TASK (result), &error);
      if (success)
	g_task_return_boolean (caller_task, TRUE);
      else
	g_task_return_error (caller_task, error);
    }
  else if (gnutls->priv->handshake_error)
    g_task_return_error (caller_task, g_error_copy (gnutls->priv->handshake_error));
  else
    g_task_return_boolean (caller_task, TRUE);

  g_object_unref (caller_task);
}

static void
async_handshake_thread (GTask        *task,
			gpointer      object,
			gpointer      task_data,
			GCancellable *cancellable)
{
  KmsGTlsConnectionGnutls *gnutls = object;

  handshake_thread (task, object, task_data, cancellable);

  g_mutex_lock (&gnutls->priv->op_mutex);
  gnutls->priv->need_finish_handshake = TRUE;
  /* yield_op will clear handshaking too, but we don't want the
   * connection to be briefly "handshaking && need_finish_handshake"
   * after we unlock the mutex.
   */
  gnutls->priv->handshaking = FALSE;
  g_mutex_unlock (&gnutls->priv->op_mutex);

  yield_op (gnutls, G_KMS_TLS_CONNECTION_GNUTLS_OP_HANDSHAKE);
}

static void
g_tls_connection_gnutls_handshake_async (GTlsConnection       *conn,
					 int                   io_priority,
					 GCancellable         *cancellable,
					 GAsyncReadyCallback   callback,
					 gpointer              user_data)
{
  GTask *thread_task, *caller_task;

  caller_task = g_task_new (conn, cancellable, callback, user_data);
  g_task_set_priority (caller_task, io_priority);

  begin_handshake (G_KMS_TLS_CONNECTION_GNUTLS (conn));

  thread_task = g_task_new (conn, cancellable,
			    handshake_thread_completed, caller_task);
  g_task_set_priority (thread_task, io_priority);
  g_task_run_in_thread (thread_task, async_handshake_thread);
  g_object_unref (thread_task);
}

static gboolean
g_tls_connection_gnutls_handshake_finish (GTlsConnection       *conn,
					  GAsyncResult         *result,
					  GError              **error)
{
  g_return_val_if_fail (g_task_is_valid (result, conn), FALSE);

  return g_task_propagate_boolean (G_TASK (result), error);
}

gboolean
g_tls_connection_gnutls_add_srtp_profile (GTlsConnection	 *conn,
					  GTlsSrtpProfile	  profile)
{
  KmsGTlsConnectionGnutls *gnutls;
  gnutls_srtp_profile_t gnutls_profile;
  gboolean ret;

  gnutls = G_KMS_TLS_CONNECTION_GNUTLS (conn);

  g_return_val_if_fail (G_IS_KMS_TLS_CONNECTION_GNUTLS (conn), FALSE);
  g_return_val_if_fail (profile != G_TLS_SRTP_PROFILE_NONE, FALSE);
  g_return_val_if_fail (g_gio_stream_is_datagram (gnutls->priv->base_io_stream),
			FALSE);

  switch (profile)
    {
    case G_TLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_80:
      gnutls_profile = GNUTLS_SRTP_AES128_CM_HMAC_SHA1_80;
      break;
    case G_TLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_32:
      gnutls_profile = GNUTLS_SRTP_AES128_CM_HMAC_SHA1_32;
      break;
    case G_TLS_SRTP_PROFILE_NULL_HMAC_SHA1_80:
      gnutls_profile = GNUTLS_SRTP_NULL_HMAC_SHA1_80;
      break;
    case G_TLS_SRTP_PROFILE_NULL_HMAC_SHA1_32:
      gnutls_profile = GNUTLS_SRTP_NULL_HMAC_SHA1_32;
      break;
    default:
      g_warning ("Profile %d is not supported by the GnuTLS backend", profile);
      return FALSE;
    }

  ret = (gnutls_srtp_set_profile (gnutls->priv->session, gnutls_profile) == GNUTLS_E_SUCCESS);

  return ret;
}

GTlsSrtpProfile
g_tls_connection_gnutls_get_selected_srtp_profile (GTlsConnection	 *conn,
						   GByteArray		**server_key,
						   GByteArray		**server_salt,
						   GByteArray		**client_key,
						   GByteArray		**client_salt)
{
  KmsGTlsConnectionGnutls *gnutls;
  gnutls_srtp_profile_t gnutls_profile;
  GTlsSrtpProfile profile;
  /* The key size is 128 bits and the master salt is 112 bits */

  gnutls = G_KMS_TLS_CONNECTION_GNUTLS (conn);

  g_return_val_if_fail (G_IS_KMS_TLS_CONNECTION_GNUTLS (conn), FALSE);
  g_return_val_if_fail (g_gio_stream_is_datagram (gnutls->priv->base_io_stream),
			FALSE);

  if (gnutls_srtp_get_selected_profile (gnutls->priv->session, &gnutls_profile)
      != GNUTLS_E_SUCCESS)
    return G_TLS_SRTP_PROFILE_NONE;

  switch (gnutls_profile)
    {
    case GNUTLS_SRTP_AES128_CM_HMAC_SHA1_80:
      profile = G_TLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_80;
      break;
    case GNUTLS_SRTP_AES128_CM_HMAC_SHA1_32:
      profile = G_TLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_32;
      break;
    case GNUTLS_SRTP_NULL_HMAC_SHA1_80:
      profile = G_TLS_SRTP_PROFILE_NULL_HMAC_SHA1_80;
      break;
    case GNUTLS_SRTP_NULL_HMAC_SHA1_32:
      profile = G_TLS_SRTP_PROFILE_NULL_HMAC_SHA1_32;
      break;
    default:
      g_warning ("Unknown SRTP profile %d returned by GnuTLS", gnutls_profile);
      return G_TLS_SRTP_PROFILE_NONE;
    }

  if (server_key != NULL || server_salt != NULL ||
      client_key != NULL || client_salt != NULL)
     {
       const guint KEY_SIZE = 18;
       const guint SALT_SIZE = 16;
       const guint BUF_SIZE = (KEY_SIZE + SALT_SIZE) * 2;
       gchar buf[BUF_SIZE];
       gnutls_datum_t client_key_datum, server_key_datum,
	 client_salt_datum, server_salt_datum;
       int ret;

       ret = gnutls_srtp_get_keys (gnutls->priv->session, buf, BUF_SIZE,
				   &client_key_datum, &client_salt_datum,
				   &server_key_datum, &server_salt_datum);
       if (ret < 0) {
	 g_warning ("Could not retrieve SRTP keys: %s", gnutls_strerror (ret));
	 return G_TLS_SRTP_PROFILE_NONE;
       }

       if (server_key)
	 {
	   *server_key = g_byte_array_sized_new (server_key_datum.size);
	   g_byte_array_append (*server_key, server_key_datum.data,
				server_key_datum.size);
	 }
       if (server_salt)
	 {
	   *server_salt = g_byte_array_sized_new (server_salt_datum.size);
	   g_byte_array_append (*server_salt, server_salt_datum.data,
				server_salt_datum.size);
	 }
       if (client_key)
	 {
	   *client_key = g_byte_array_sized_new (client_key_datum.size);
	   g_byte_array_append (*client_key, client_key_datum.data,
				client_key_datum.size);
	 }
       if (client_salt)
	 {
	   *client_salt = g_byte_array_sized_new (client_salt_datum.size);
	   g_byte_array_append (*client_salt, client_salt_datum.data,
				client_salt_datum.size);
	 }

     }

  return profile;
}

static gboolean
do_implicit_handshake (KmsGTlsConnectionGnutls  *gnutls,
		       gboolean               blocking,
		       GCancellable          *cancellable,
		       GError               **error)
{
  /* We have op_mutex */
  gnutls->priv->implicit_handshake = g_task_new (gnutls, cancellable, NULL, NULL);

  begin_handshake (gnutls);

  if (blocking)
    {
      GError *my_error = NULL;
      gboolean success;

      g_mutex_unlock (&gnutls->priv->op_mutex);
      g_task_run_in_thread_sync (gnutls->priv->implicit_handshake,
				 handshake_thread);
      success = finish_handshake (gnutls,
				  gnutls->priv->implicit_handshake,
				  &my_error);
      g_clear_object (&gnutls->priv->implicit_handshake);
      yield_op (gnutls, G_KMS_TLS_CONNECTION_GNUTLS_OP_HANDSHAKE);
      g_mutex_lock (&gnutls->priv->op_mutex);

      if (my_error)
	g_propagate_error (error, my_error);
      return success;
    }
  else
    {
      g_task_run_in_thread (gnutls->priv->implicit_handshake,
			    async_handshake_thread);

      g_set_error_literal (error, G_IO_ERROR, G_IO_ERROR_WOULD_BLOCK,
			   _("Operation would block"));

      return FALSE;
    }
}

gssize
g_tls_connection_gnutls_read (KmsGTlsConnectionGnutls  *gnutls,
			      void                  *buffer,
			      gsize                  count,
			      gboolean               blocking,
			      GCancellable          *cancellable,
			      GError               **error)
{
  gssize ret;

  if (gnutls->priv->app_data_buf && !gnutls->priv->handshaking)
    {
      ret = MIN (count, gnutls->priv->app_data_buf->len);
      memcpy (buffer, gnutls->priv->app_data_buf->data, ret);
      if (ret == gnutls->priv->app_data_buf->len)
	g_clear_pointer (&gnutls->priv->app_data_buf, g_byte_array_unref);
      else
	g_byte_array_remove_range (gnutls->priv->app_data_buf, 0, ret);
      return ret;
    }

 again:
  if (!claim_op (gnutls, G_KMS_TLS_CONNECTION_GNUTLS_OP_READ,
		 blocking, cancellable, error))
    return -1;

  BEGIN_GNUTLS_IO (gnutls, G_IO_IN, blocking, cancellable);
  ret = gnutls_record_recv (gnutls->priv->session, buffer, count);
  END_GNUTLS_IO (gnutls, G_IO_IN, ret, _("Error reading data from TLS socket: %s"), error);

  yield_op (gnutls, G_KMS_TLS_CONNECTION_GNUTLS_OP_READ);

  if (ret >= 0)
    return ret;
  else if (ret == GNUTLS_E_REHANDSHAKE)
    goto again;
  else
    return -1;
}

gssize
g_tls_connection_gnutls_write (KmsGTlsConnectionGnutls  *gnutls,
			       const void            *buffer,
			       gsize                  count,
			       gboolean               blocking,
			       GCancellable          *cancellable,
			       GError               **error)
{
  gssize ret;

 again:
  if (!claim_op (gnutls, G_KMS_TLS_CONNECTION_GNUTLS_OP_WRITE,
		 blocking, cancellable, error))
    return -1;

  if (gnutls_dtls_get_data_mtu (gnutls->priv->session) < count) {
    ret = GNUTLS_E_LARGE_PACKET;
#if 0 /* glib */
    g_set_error (error, G_IO_ERROR, G_IO_ERROR_MESSAGE_TOO_LARGE,
		 "Trying to write %" G_GSIZE_FORMAT " bytes, but MTU is %u",
		 count, gnutls_dtls_get_data_mtu (gnutls->priv->session));
#else
    g_set_error (error, G_IO_ERROR, G_IO_ERROR_INVALID_DATA,
                 "Trying to write %" G_GSIZE_FORMAT " bytes, but MTU is %u",
                 count, gnutls_dtls_get_data_mtu (gnutls->priv->session));
#endif
    goto done;
  }

  BEGIN_GNUTLS_IO (gnutls, G_IO_OUT, blocking, cancellable);
  ret = gnutls_record_send (gnutls->priv->session, buffer, count);
  END_GNUTLS_IO (gnutls, G_IO_OUT, ret, _("Error writing data to TLS socket: %s"), error);

 done:
  yield_op (gnutls, G_KMS_TLS_CONNECTION_GNUTLS_OP_WRITE);

  if (ret >= 0)
    return ret;
  else if (ret == GNUTLS_E_REHANDSHAKE)
    goto again;
  else
    return -1;
}

static GInputStream  *
g_tls_connection_gnutls_get_input_stream (GIOStream *stream)
{
  KmsGTlsConnectionGnutls *gnutls = G_KMS_TLS_CONNECTION_GNUTLS (stream);

  return gnutls->priv->tls_istream;
}

static GOutputStream *
g_tls_connection_gnutls_get_output_stream (GIOStream *stream)
{
  KmsGTlsConnectionGnutls *gnutls = G_KMS_TLS_CONNECTION_GNUTLS (stream);

  return gnutls->priv->tls_ostream;
}

static gboolean
g_tls_connection_gnutls_close (GIOStream     *stream,
			       GCancellable  *cancellable,
			       GError       **error)
{
  KmsGTlsConnectionGnutls *gnutls = G_KMS_TLS_CONNECTION_GNUTLS (stream);
  gboolean success;
  int ret = 0;

  if (!claim_op (gnutls, G_KMS_TLS_CONNECTION_GNUTLS_OP_CLOSE,
		 TRUE, cancellable, error))
    return FALSE;

  if (gnutls->priv->closed)
    {
      g_set_error_literal (error, G_IO_ERROR, G_IO_ERROR_CLOSED,
			   _("Connection is already closed"));
      yield_op (gnutls, G_KMS_TLS_CONNECTION_GNUTLS_OP_CLOSE);
      return FALSE;
    }

  if (gnutls->priv->ever_handshaked)
    {
      BEGIN_GNUTLS_IO (gnutls, G_IO_IN | G_IO_OUT, TRUE, cancellable);
      ret = gnutls_bye (gnutls->priv->session, GNUTLS_SHUT_WR);
      END_GNUTLS_IO (gnutls, G_IO_IN | G_IO_OUT, ret,
		     _("Error performing TLS close: %s"), error);
    }

  gnutls->priv->closed = TRUE;

  g_object_notify (G_OBJECT (gnutls), "status");

  if (ret != 0)
    {
      yield_op (gnutls, G_KMS_TLS_CONNECTION_GNUTLS_OP_CLOSE);
      return FALSE;
    }

  success = g_io_stream_close (gnutls->priv->base_io_stream,
			       cancellable, error);
  yield_op (gnutls, G_KMS_TLS_CONNECTION_GNUTLS_OP_CLOSE);
  return success;
}

/* We do async close as synchronous-in-a-thread so we don't need to
 * implement G_IO_IN/G_IO_OUT flip-flopping just for this one case
 * (since handshakes are also done synchronously now).
 */
static void
close_thread (GTask        *task,
	      gpointer      object,
	      gpointer      task_data,
	      GCancellable *cancellable)
{
  GIOStream *stream = object;
  GError *error = NULL;

  if (!g_tls_connection_gnutls_close (stream, cancellable, &error))
    g_task_return_error (task, error);
  else
    g_task_return_boolean (task, TRUE);
}

static void
g_tls_connection_gnutls_close_async (GIOStream           *stream,
				     int                  io_priority,
				     GCancellable        *cancellable,
				     GAsyncReadyCallback  callback,
				     gpointer             user_data)
{
  GTask *task;

  task = g_task_new (stream, cancellable, callback, user_data);
  g_task_set_priority (task, io_priority);
  g_task_run_in_thread (task, close_thread);
  g_object_unref (task);
}

static gboolean
g_tls_connection_gnutls_close_finish (GIOStream           *stream,
				      GAsyncResult        *result,
				      GError             **error)
{
  g_return_val_if_fail (g_task_is_valid (result, stream), FALSE);

  return g_task_propagate_boolean (G_TASK (result), error);
}

static gboolean
g_gio_stream_is_datagram (GIOStream *stream)
{
  return TRUE;
}

#if 0 /* glib */
static gboolean
g_tls_connection_gnutls_is_datagram (GIOStream            *stream)
{
  KmsGTlsConnectionGnutls *gnutls;

  g_return_val_if_fail (G_IS_KMS_TLS_CONNECTION_GNUTLS (stream), FALSE);

  gnutls = G_KMS_TLS_CONNECTION_GNUTLS (stream);

  return g_gio_stream_is_datagram (gnutls->priv->base_io_stream);
}
#endif

#ifdef HAVE_PKCS11

static P11KitPin*
on_pin_prompt_callback (const char     *pinfile,
                        P11KitUri      *pin_uri,
                        const char     *pin_description,
                        P11KitPinFlags  pin_flags,
                        void           *callback_data)
{
  KmsGTlsConnectionGnutls *gnutls = G_KMS_TLS_CONNECTION_GNUTLS (callback_data);
  GTlsInteractionResult result;
  GTlsPasswordFlags flags = 0;
  GTlsPassword *password;
  P11KitPin *pin = NULL;
  GError *error = NULL;

  if (!gnutls->priv->interaction)
    return NULL;

  if (pin_flags & P11_KIT_PIN_FLAGS_RETRY)
    flags |= G_TLS_PASSWORD_RETRY;
  if (pin_flags & P11_KIT_PIN_FLAGS_MANY_TRIES)
    flags |= G_TLS_PASSWORD_MANY_TRIES;
  if (pin_flags & P11_KIT_PIN_FLAGS_FINAL_TRY)
    flags |= G_TLS_PASSWORD_FINAL_TRY;

  password = g_pkcs11_pin_new (flags, pin_description);

  result = g_tls_interaction_ask_password (gnutls->priv->interaction, password,
                                           g_cancellable_get_current (), &error);

  switch (result)
    {
    case G_TLS_INTERACTION_FAILED:
      if (!g_error_matches (error, G_IO_ERROR, G_IO_ERROR_CANCELLED))
        g_warning ("couldn't ask for password: %s", error->message);
      pin = NULL;
      break;
    case G_TLS_INTERACTION_UNHANDLED:
      pin = NULL;
      break;
    case G_TLS_INTERACTION_HANDLED:
      pin = g_pkcs11_pin_steal_internal (G_PKCS11_PIN (password));
      break;
    }

  g_object_unref (password);
  return pin;
}

#endif /* HAVE_PKCS11 */

static void
g_tls_connection_gnutls_class_init (KmsGTlsConnectionGnutlsClass *klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GTlsConnectionClass *connection_class = G_TLS_CONNECTION_CLASS (klass);
  GIOStreamClass *iostream_class = G_IO_STREAM_CLASS (klass);

  g_type_class_add_private (klass, sizeof (KmsGTlsConnectionGnutlsPrivate));

  gobject_class->get_property = g_tls_connection_gnutls_get_property;
  gobject_class->set_property = g_tls_connection_gnutls_set_property;
  gobject_class->finalize     = g_tls_connection_gnutls_finalize;

  connection_class->handshake        = g_tls_connection_gnutls_handshake;
  connection_class->handshake_async  = g_tls_connection_gnutls_handshake_async;
  connection_class->handshake_finish = g_tls_connection_gnutls_handshake_finish;
#if 0 /* glib */
  connection_class->add_srtp_profile = g_tls_connection_gnutls_add_srtp_profile;
  connection_class->get_selected_srtp_profile = g_tls_connection_gnutls_get_selected_srtp_profile;
#endif
  iostream_class->get_input_stream  = g_tls_connection_gnutls_get_input_stream;
  iostream_class->get_output_stream = g_tls_connection_gnutls_get_output_stream;
  iostream_class->close_fn          = g_tls_connection_gnutls_close;
  iostream_class->close_async       = g_tls_connection_gnutls_close_async;
  iostream_class->close_finish      = g_tls_connection_gnutls_close_finish;
#if 0 /* glib */
  iostream_class->is_datagram       = g_tls_connection_gnutls_is_datagram;
#endif

  g_object_class_override_property (gobject_class, PROP_BASE_IO_STREAM, "base-io-stream");
  g_object_class_override_property (gobject_class, PROP_REQUIRE_CLOSE_NOTIFY, "require-close-notify");
  g_object_class_override_property (gobject_class, PROP_REHANDSHAKE_MODE, "rehandshake-mode");
  g_object_class_override_property (gobject_class, PROP_USE_SYSTEM_CERTDB, "use-system-certdb");
  g_object_class_override_property (gobject_class, PROP_DATABASE, "database");
  g_object_class_override_property (gobject_class, PROP_CERTIFICATE, "certificate");
  g_object_class_override_property (gobject_class, PROP_INTERACTION, "interaction");
  g_object_class_override_property (gobject_class, PROP_PEER_CERTIFICATE, "peer-certificate");
  g_object_class_override_property (gobject_class, PROP_PEER_CERTIFICATE_ERRORS, "peer-certificate-errors");

#if 0 /* glib */
  g_object_class_override_property (gobject_class, PROP_STATUS, "status");
#else
  /**
   * GTlsConnection:status:
   *
   * This is the status of the connection. The connection can go back
   * to #G_TLS_STATUS_REHANSHAKING if g_tls_connection_handshake() or
   * g_tls_connection_handshake_async() are called or if the peer has
   * requested a new handshake. When the connection is in state
   * #G_TLS_STATUS_CLOSED, most functions on the object will fail.
   *
   * Since: 2.38
   */
  g_object_class_install_property (gobject_class, PROP_STATUS,
                                  g_param_spec_enum ("status",
                                                     "Connection status",
                                                     "The status of the TLS connection",
                                                     G_TYPE_TLS_STATUS,
                                                     G_TLS_STATUS_NEW,
                                                     G_PARAM_READABLE |
                                                     G_PARAM_STATIC_STRINGS));
#endif
}

static void
g_tls_connection_gnutls_initable_iface_init (GInitableIface *iface)
{
  iface->init = g_tls_connection_gnutls_initable_init;
}
