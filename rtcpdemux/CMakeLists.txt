include_directories(
  ${GSTREAMER_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
)

set(RTCPDEMUX_SOURCES
  rtcpdemux.c
  kmsrtcpdemux.c
  kmsrtcpdemux.h
)

add_library(rtcpdemux MODULE ${RTCPDEMUX_SOURCES})

target_link_libraries(rtcpdemux
  ${GSTREAMER_LIBRARIES}
)

install(
  TARGETS rtcpdemux
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_MODULESDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
