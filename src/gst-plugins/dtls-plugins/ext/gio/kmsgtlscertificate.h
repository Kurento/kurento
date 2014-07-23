
#ifndef __KMS_G_TLS_CERTIFICATE__
#define __KMS_G_TLS_CERTIFICATE__

#include <gio/gio.h>

GTlsCertificate *
kms_g_tls_certificate_new_from_file (const gchar  *file,
                                     GError      **error);
GList *
kms_g_tls_certificate_list_new_from_file (const gchar  *file,
                                          GError      **error);

#endif /* __KMS_G_TLS_CERTIFICATE__ */
