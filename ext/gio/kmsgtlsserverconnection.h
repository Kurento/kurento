
#ifndef __KMS_G_TLS_SERVER_CONNECTION__
#define __KMS_G_TLS_SERVER_CONNECTION__

#include <gio/gio.h>

GIOStream *
kms_g_tls_server_connection_new (GIOStream        *base_io_stream,
                                 GTlsCertificate  *certificate,
                                 GError          **error);

#endif /* __KMS_G_TLS_SERVER_CONNECTION__ */
