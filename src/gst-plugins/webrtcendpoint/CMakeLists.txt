cmake_minimum_required(VERSION 2.8)

set (CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC")

set(CUSTOM_PREFIX "kurento")
set(INCLUDE_PREFIX "${CMAKE_INSTALL_INCLUDEDIR}/${CUSTOM_PREFIX}/webrtcendpoint")

set(KMS_ICE_SOURCES
  kmsicecandidate.c
  kmsicebaseagent.c
  kmsiceniceagent.c
)

set(KMS_ICE_HEADERS
  kmsicecandidate.h
  kmsicebaseagent.h
  kmsiceniceagent.h
)

set(KMS_WEBRTC_DATA_PROTOCOL_SOURCES
  kmswebrtcdatasessionbin.c
  kmswebrtcdatachannelbin.c
  kmswebrtcdatachannel.c
)

set(KMS_WEBRTC_DATA_PROTOCOL_HEADERS
  kmswebrtcdatasessionbin.h
  kmswebrtcdatachannelbin.h
  kmswebrtcdatachannel.h
  kmswebrtcdatachannelstate.h
  kmswebrtcdatachannelpriority.h
  kmswebrtcdataproto.h
  kmswebrtcdatachannelutil.h
)

set(KMS_WEBRTC_DATA_PROTOCOL_ENUM_HEADERS
  kmswebrtcdatachannelstate.h
  kmswebrtcdatachannelpriority.h
)

add_glib_enumtypes (
  KMS_WEBRTC_DATA_PROTOCOL_SOURCES
  KMS_WEBRTC_DATA_PROTOCOL_HEADERS
  kms-webrtc-enumtypes
  KMS
  ${KMS_WEBRTC_DATA_PROTOCOL_ENUM_HEADERS}
)

add_glib_marshal (
  KMS_WEBRTC_DATA_PROTOCOL_SOURCES
  KMS_WEBRTC_DATA_PROTOCOL_HEADERS
  kms-webrtc-data-marshal
  __kms_webrtc_data_marshal
)

add_library(webrtcdataproto SHARED
  ${KMS_WEBRTC_DATA_PROTOCOL_SOURCES}
  ${KMS_WEBRTC_DATA_PROTOCOL_HEADERS}
)

target_link_libraries(webrtcdataproto
  ${gstreamer-1.5_LIBRARIES}
  ${gstreamer-base-1.5_LIBRARIES}
  ${gstreamer-app-1.5_LIBRARIES}
  ${gstreamer-sctp-1.5_LIBRARIES}
)

set_target_properties(webrtcdataproto PROPERTIES PUBLIC_HEADER "${KMS_WEBRTC_DATA_PROTOCOL_HEADERS}")
set_target_properties(webrtcdataproto PROPERTIES VERSION ${PROJECT_VERSION} SOVERSION ${PROJECT_VERSION_MAJOR})

install(
  TARGETS webrtcdataproto
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
  PUBLIC_HEADER DESTINATION ${INCLUDE_PREFIX}
)

set(KMS_WEBRTC_SOURCES
  kmswebrtcbaseconnection.c
  kmswebrtcconnection.c
  kmswebrtcrtcpmuxconnection.c
  kmswebrtcbundleconnection.c
  kmswebrtcsctpconnection.c
  kmswebrtctransportsrcnice.c
  kmswebrtctransportsinknice.c
  kmswebrtctransportsrc.c
  kmswebrtctransportsink.c
  kmswebrtctransport.c
  kmswebrtcsession.c
  kmswebrtcendpoint.c
  ${KMS_ICE_SOURCES}
)

set(KMS_WEBRTC_HEADERS
  kmswebrtcbaseconnection.h
  kmswebrtcconnection.h
  kmswebrtcrtcpmuxconnection.h
  kmswebrtcbundleconnection.h
  kmswebrtcsctpconnection.h
  kmswebrtctransportsrc.h
  kmswebrtctransportsink.h
  kmswebrtctransport.h
  kmswebrtcsession.h
  kmswebrtcendpoint.h
  ${KMS_ICE_HEADERS}
)

add_glib_marshal(KMS_WEBRTC_SOURCES KMS_WEBRTC_HEADERS kms-webrtc-marshal __kms_webrtc_marshal)

add_library(kmswebrtcendpointlib SHARED ${KMS_WEBRTC_SOURCES} ${KMS_WEBRTC_HEADERS})
add_dependencies(kmswebrtcendpointlib rtcpdemux webrtcdataproto)

target_link_libraries(kmswebrtcendpointlib
  webrtcdataproto
  ${KmsGstCommons_LIBRARIES}
  ${gstreamer-1.5_LIBRARIES}
  ${gstreamer-base-1.5_LIBRARIES}
  ${gstreamer-pbutils-1.5_LIBRARIES}
  ${nice_LIBRARIES}
)

set_target_properties(kmswebrtcendpointlib PROPERTIES PUBLIC_HEADER "${KMS_WEBRTC_HEADERS}")
set_target_properties(kmswebrtcendpointlib PROPERTIES VERSION ${PROJECT_VERSION} SOVERSION ${PROJECT_VERSION_MAJOR})

install(
  TARGETS kmswebrtcendpointlib
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
  PUBLIC_HEADER DESTINATION ${INCLUDE_PREFIX}
)

foreach (HEADER ${KMS_WEBRTC_HEADERS})
  list (APPEND FIND_HEADERS webrtcendpoint/${HEADER})
endforeach ()

set (REQUIRED_LIBS
  "nice ${NICE_REQUIRED}"
)

configure_file(FindKmsWebRtcEndpointLib.cmake.in ${CMAKE_BINARY_DIR}/FindKmsWebRtcEndpointLib.cmake @ONLY)

install(FILES
  ${CMAKE_BINARY_DIR}/FindKmsWebRtcEndpointLib.cmake
  DESTINATION ${CMAKE_MODULES_INSTALL_DIR}
)

add_library(webrtcendpoint MODULE kmswebrtc.c)
add_dependencies(webrtcendpoint kmswebrtcendpointlib)

include_directories(
  ${KmsGstCommons_INCLUDE_DIRS}
  ${gstreamer-1.5_INCLUDE_DIRS}
  ${gstreamer-base-1.5_INCLUDE_DIRS}
  ${gstreamer-pbutils-1.5_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${CMAKE_CURRENT_BINARY_DIR}
  ${nice_INCLUDE_DIRS}
)

target_link_libraries(webrtcendpoint
  kmswebrtcendpointlib
)

install(
  TARGETS webrtcendpoint
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
