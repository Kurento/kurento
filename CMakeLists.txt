add_subdirectory(rtcpdemux)

include_directories(
  ${GSTREAMER_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
)

set(KMS_DTLS_SOURCES
  gstdtls.c
  gstdtlsenc.c
  gstdtlsdec.c
  gstdtlsbase.c
  gstiostream.c
  gstdtlsconnection.c
  gstdtlssrtpdemux.c
  gstdtlssrtpdec.c
  gstdtlssrtpenc.c
  ext/gio/kmsgtlsconnection.c
  ext/gio/kmsgtlsclientconnection.c
  ext/gio/kmsgtlsserverconnection.c
  ext/gio/kmsgioenumtypes.c
  ext/gio/kmsgtlscertificate.c
  ext/gnutls/gtlsbackend-gnutls.c
  ext/gnutls/gtlsbackend-gnutls.h
  ext/gnutls/gtlscertificate-gnutls.c
  ext/gnutls/gtlscertificate-gnutls.h
  ext/gnutls/gtlsclientconnection-gnutls.c
  ext/gnutls/gtlsclientconnection-gnutls.h
  ext/gnutls/gtlsconnection-gnutls.c
  ext/gnutls/gtlsconnection-gnutls.h
  ext/gnutls/gtlsdatabase-gnutls.c
  ext/gnutls/gtlsdatabase-gnutls.h
  ext/gnutls/gtlsfiledatabase-gnutls.c
  ext/gnutls/gtlsfiledatabase-gnutls.h
  ext/gnutls/gtlsinputstream-gnutls.c
  ext/gnutls/gtlsinputstream-gnutls.h
  ext/gnutls/gtlsoutputstream-gnutls.c
  ext/gnutls/gtlsoutputstream-gnutls.h
  ext/gnutls/gtlsserverconnection-gnutls.c
  ext/gnutls/gtlsserverconnection-gnutls.h
)

include(GLibHelpers)

set(ENUM_HEADERS
   gstdtlssrtp.h
)

add_glib_enumtypes(KMS_DTLS_SOURCES gstdtls-enumtypes GST_DTLS ${ENUM_HEADERS})

add_library(kmsdtls MODULE ${KMS_DTLS_SOURCES})

target_link_libraries(kmsdtls
  ${GSTREAMER_LIBRARIES}
  ${GNUTLS_LIBRARIES}
  ${GIO_LIBRARIES}
)

install(
  TARGETS kmsdtls
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
