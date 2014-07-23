
#ifndef __KMS_GTLSCONNECTION_H__
#define __KMS_GTLSCONNECTION_H__

#include <gio/gio.h>
#include "kmsgioenums.h"

gboolean g_tls_connection_add_srtp_profile (GTlsConnection       *conn,
                                            GTlsSrtpProfile       profile);

GTlsSrtpProfile g_tls_connection_get_selected_srtp_profile (GTlsConnection       *conn,
                                                            GByteArray          **server_key,
                                                            GByteArray          **server_salt,
                                                            GByteArray          **client_key,
                                                            GByteArray          **client_salt);

#endif /* __KMS_GTLSCONNECTION_H__ */
