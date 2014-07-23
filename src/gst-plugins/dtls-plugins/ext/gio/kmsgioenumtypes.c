
#include "kmsgioenumtypes.h"

#include "kmsgioenums.h"

GType
g_tls_status_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;

  if (g_once_init_enter (&g_define_type_id__volatile))
    {
      static const GEnumValue values[] = {
        { G_TLS_STATUS_NEW, "G_TLS_STATUS_NEW", "new" },
        { G_TLS_STATUS_HANDSHAKING, "G_TLS_STATUS_HANDSHAKING", "handshaking" },
        { G_TLS_STATUS_CONNECTED, "G_TLS_STATUS_CONNECTED", "connected" },
        { G_TLS_STATUS_REHANDSHAKING, "G_TLS_STATUS_REHANDSHAKING", "rehandshaking" },
        { G_TLS_STATUS_CLOSED, "G_TLS_STATUS_CLOSED", "closed" },
        { G_TLS_STATUS_ERROR, "G_TLS_STATUS_ERROR", "error" },
        { 0, NULL, NULL }
      };
      GType g_define_type_id =
        g_enum_register_static (g_intern_static_string ("GTlsStatus"), values);
      g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
    }

  return g_define_type_id__volatile;
}
