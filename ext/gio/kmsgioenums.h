
#ifndef __KMS_GIO_ENUMS_H__
#define __KMS_GIO_ENUMS_H__

/**
 * GTlsSrtpProfile:
 * @G_TLS_SRTP_PROFILE_NONE: No SRTP profile has been selected
 * @G_TLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_80: AES-128-CM cipher with HMAC-SHA1-80 authentication function
 * @G_TLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_32: AES-128-CM cipher with HMAC-SHA1-32 authentication function
 * @G_TLS_SRTP_PROFILE_NULL_HMAC_SHA1_80: No encryption cipher with HMAC-SHA1-80 authentication function
 * @G_TLS_SRTP_PROFILE_NULL_HMAC_SHA1_32: No encryption cipher with HMAC-SHA1-32 authentication function
 *
 * Possible SRTP profiles for DTLS-SRTP negotiation, corresponds to the
 * "DTLS-SRTP Protection Profiles" IANA registry.
 *
 * Since: 2.38
 */

typedef enum {
  G_TLS_SRTP_PROFILE_NONE = 0,
  G_TLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_80 = 1,
  G_TLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_32 = 2,
  G_TLS_SRTP_PROFILE_NULL_HMAC_SHA1_80 = 5,
  G_TLS_SRTP_PROFILE_NULL_HMAC_SHA1_32 = 6,
} GTlsSrtpProfile;

/**
 * GTlsStatus:
 * @G_TLS_STATUS_NEW: New connection, has never been connected
 * @G_TLS_STATUS_HANDSHAKING: Performing the initial TLS handshake
 * @G_TLS_STATUS_CONNECTED: The TLS connection has been succesfully performed
 * @G_TLS_STATUS_REHANSHAKING: The TLS connection is performing a new handshake
 * @G_TLS_STATUS_CLOSED: The TLS connection has been closed
 * @G_TLS_STATUS_ERROR: The TLS handshake has ended with an error
 *
 * Describes the state of a #GTlsConnection
 *
 * Since: 2.38
 */

typedef enum {
  G_TLS_STATUS_NEW,
  G_TLS_STATUS_HANDSHAKING,
  G_TLS_STATUS_CONNECTED,
  G_TLS_STATUS_REHANDSHAKING,
  G_TLS_STATUS_CLOSED,
  G_TLS_STATUS_ERROR
} GTlsStatus;

#endif /* __KMS_GIO_ENUMS_H__ */
