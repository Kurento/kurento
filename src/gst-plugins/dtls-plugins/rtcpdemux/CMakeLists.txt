include_directories(
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${CMAKE_CURRENT_BINARY_DIR}
)

set(RTCPDEMUX_SOURCES
  rtcpdemux.c
  kmsrtcpdemux.c
)

set(RTCPDEMUX_HEADERS
  kmsrtcpdemux.h
)

include(GLibHelpers)

add_glib_marshal(RTCPDEMUX_SOURCES RTCPDEMUX_HEADERS kms-marshal __kms_marshal)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Werror -Wall")

add_library(rtcpdemux MODULE ${RTCPDEMUX_SOURCES} ${RTCPDEMUX_HEADERS})

target_link_libraries(rtcpdemux
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-base-1.0_LIBRARIES}
  ${gstreamer-rtp-1.0_LIBRARIES}
)

install(
  TARGETS rtcpdemux
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
