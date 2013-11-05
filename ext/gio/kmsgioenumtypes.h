
#ifndef __KMS_GIO_ENUM_TYPES_H__
#define __KMS_GIO_ENUM_TYPES_H__

#include <gio/gio.h>

G_BEGIN_DECLS

GLIB_AVAILABLE_IN_ALL GType g_tls_status_get_type (void) G_GNUC_CONST;
#define G_TYPE_TLS_STATUS (g_tls_status_get_type ())

G_END_DECLS

#endif /* __KMS_GIO_ENUM_TYPES_H__ */
