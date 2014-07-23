
#ifndef __KMS_G_TLS_CLIENT_CONNECTION__
#define __KMS_G_TLS_CLIENT_CONNECTION__

#include <gio/gio.h>

GIOStream * kms_g_tls_client_connection_new (GIOStream           *base_io_stream,
                                            GSocketConnectable  *server_identity,
                                            GError             **error);

#endif /* __KMS_G_TLS_CLIENT_CONNECTION__ */
