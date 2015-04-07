include_directories(
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${gstreamer-video-1.0_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${opencv_INCLUDE_DIRS}
  ${libsoup-2.4_INCLUDE_DIRS}
)

set(LOGOOVERLAY_SOURCES
  logooverlay.c
  kmslogooverlay.c kmslogooverlay.h
)

add_library(logooverlay MODULE ${LOGOOVERLAY_SOURCES})

target_link_libraries(logooverlay
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${opencv_LIBRARIES}
  ${libsoup-2.4_LIBRARIES}
)

install(
  TARGETS logooverlay
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
