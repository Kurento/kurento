cmake_minimum_required(VERSION 2.8)

set(KMS_RTPENDPOINT_SOURCES
  kmsrtpbaseconnection.c
  kmsrtpconnection.c
  kmssrtpconnection.c
  kmsrtpsession.c
  kmssrtpsession.c
  kmsrtpendpoint.c
  kmssocketutils.c
)

set(KMS_RTPENDPOINT_HEADERS
  kmsrtpendpoint.h
  kmssocketutils.h
)

set(ENUM_HEADERS
  kmsrtpsdescryptosuite.h
)

list(APPEND KMS_RTPENDPOINT_HEADERS ${ENUM_HEADERS})
add_glib_enumtypes(KMS_RTPENDPOINT_SOURCES KMS_RTPENDPOINT_HEADERS kms-rtp-enumtypes KMS ${ENUM_HEADERS})

add_library(rtpendpoint MODULE ${KMS_RTPENDPOINT_SOURCES} ${KMS_RTPENDPOINT_HEADERS})

add_dependencies(rtpendpoint rtcpdemux)

include_directories(
  ${KmsGstCommons_INCLUDE_DIRS}
  ${gstreamer-1.5_INCLUDE_DIRS}
  ${gstreamer-base-1.5_INCLUDE_DIRS}
  ${gstreamer-sdp-1.5_INCLUDE_DIRS}
  ${gstreamer-pbutils-1.5_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${CMAKE_CURRENT_BINARY_DIR}
  ${nice_INCLUDE_DIRS}
)

target_link_libraries(rtpendpoint
  ${KmsGstCommons_LIBRARIES}
  ${gstreamer-1.5_LIBRARIES}
  ${gstreamer-base-1.5_LIBRARIES}
  ${gstreamer-sdp-1.5_LIBRARIES}
  ${gstreamer-pbutils-1.5_LIBRARIES}
  ${nice_LIBRARIES}
)

install(
  TARGETS rtpendpoint
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
