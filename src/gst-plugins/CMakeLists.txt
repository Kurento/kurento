cmake_minimum_required(VERSION 2.8)

include(GLibHelpers)

add_subdirectory(dtls-plugins)

set(KMS_ELEMENTS_SOURCES
  kmselements.c
  kmsconfcontroller.c
  kmshttpendpoint.c
  kmsplayerendpoint.c
  kmsrecorderendpoint.c
  kmsrtpendpoint.c
  kmswebrtcendpoint.c
  kmsselectablemixer.c
  kmsdispatcher.c
  kmsdispatcheronetomany.c
  kmscompositemixer.c
  gstsctp.c
  gstsctpclientsink.c
  gstsctpserversrc.c
  kmssctpconnection.c
  kmssctpbaserpc.c
  kmssctpclientrpc.c
  kmssctpserverrpc.c
  kmsalphablending.c
  kmsplumberendpoint.c
  kmsmultichannelcontroller.c
  kmsmuxingpipeline.c
)

set(KMS_ELEMENTS_HEADERS
  kmsconfcontroller.h
  kmshttpendpoint.h
  kmshttpendpointmethod.h
  kmsplayerendpoint.h
  kmsrecorderendpoint.h
  kmsrtpendpoint.h
  kmswebrtcendpoint.h
  kmsselectablemixer.h
  kmsdispatcher.h
  kmsdispatcheronetomany.h
  kmscompositemixer.h
  gstsctp.h
  gstsctpclientsink.h
  gstsctpserversrc.h
  kmssctpconnection.h
  kmssctpbaserpc.h
  kmssctpclientrpc.h
  kmssctpserverrpc.h
  kmsalphablending.h
  kmsplumberendpoint.h
  kmsmultichannelcontroller.h
  kmsmccp.h
  kmsmuxingpipeline.h
)

set(ENUM_HEADERS
  kmshttpendpointmethod.h
  kmsencodingrules.h
)

add_glib_marshal(KMS_ELEMENTS_SOURCES KMS_ELEMENTS_HEADERS kms-elements-marshal __kms_elements_marshal)
add_glib_enumtypes(KMS_ELEMENTS_SOURCES KMS_ELEMENTS_HEADERS kms-elements-enumtypes KMS ${ENUM_HEADERS})

add_library(${LIBRARY_NAME}plugins MODULE ${KMS_ELEMENTS_SOURCES} ${KMS_ELEMENTS_HEADERS})

add_dependencies(${LIBRARY_NAME}plugins kmsdtls rtcpdemux)

include_directories(
  ${KmsGstCommons_INCLUDE_DIRS}
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${gstreamer-base-1.0_SOURCE_DIRS}
  ${gstreamer-sdp-1.0_SOURCE_DIRS}
  ${gstreamer-pbutils-1.0_SOURCE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${CMAKE_CURRENT_BINARY_DIR}
  ${libsoup-2.4_INCLUDE_DIRS}
  ${nice_INCLUDE_DIRS}
  ${SCTP_INCLUDE_DIRS}
  ${GstMarshal_INCLUDE_DIRS}
)

target_link_libraries(${LIBRARY_NAME}plugins
  ${KmsGstCommons_LIBRARIES}
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-base-1.0_LIBRARIES}
  ${gstreamer-sdp-1.0_LIBRARIES}
  ${gstreamer-pbutils-1.0_LIBRARIES}
  ${libsoup-2.4_LIBRARIES}
  ${nice_LIBRARIES}
  ${SCTP_LIBRARIES}
  ${GstMarshal_LIBRARIES}
)

install(
  TARGETS ${LIBRARY_NAME}plugins
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
