cmake_minimum_required(VERSION 2.8)

set(KMS_WEBRTC_SOURCES
  kmswebrtcconnection.c
  kmswebrtcendpoint.c
)

set(KMS_WEBRTC_HEADERS
  kmswebrtcendpoint.h
)

add_library(webrtcendpoint MODULE ${KMS_WEBRTC_SOURCES} ${KMS_WEBRTC_HEADERS})

add_dependencies(webrtcendpoint kmsdtls rtcpdemux)

include_directories(
  ${KmsGstCommons_INCLUDE_DIRS}
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${gstreamer-base-1.0_SOURCE_DIRS}
  ${gstreamer-pbutils-1.0_SOURCE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${CMAKE_CURRENT_BINARY_DIR}
  ${nice_INCLUDE_DIRS}
)

target_link_libraries(webrtcendpoint
  ${KmsGstCommons_LIBRARIES}
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-base-1.0_LIBRARIES}
  ${gstreamer-pbutils-1.0_LIBRARIES}
  ${nice_LIBRARIES}
)

install(
  TARGETS webrtcendpoint
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
