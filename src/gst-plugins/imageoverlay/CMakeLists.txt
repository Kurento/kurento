include_directories(
  ${gstreamer-1.5_INCLUDE_DIRS}
  ${gstreamer-video-1.5_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${opencv_INCLUDE_DIRS}
  ${libsoup-2.4_INCLUDE_DIRS}
)

set(IMAGEOVERLAY_SOURCES
  imageoverlay.c
  kmsimageoverlay.c kmsimageoverlay.h
)

add_library(imageoverlay MODULE ${IMAGEOVERLAY_SOURCES})

target_link_libraries(imageoverlay
  ${gstreamer-1.5_LIBRARIES}
  ${gstreamer-video-1.5_LIBRARIES}
  ${opencv_LIBRARIES}
  ${libsoup-2.4_LIBRARIES}
)

install(
  TARGETS imageoverlay
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
