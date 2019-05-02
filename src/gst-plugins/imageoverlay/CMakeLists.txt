set(IMAGEOVERLAY_SOURCES
  imageoverlay.c
  kmsimageoverlay.c kmsimageoverlay.h
)

add_library(kmsimageoverlay MODULE ${IMAGEOVERLAY_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(imageoverlay)
endif()

target_link_libraries(kmsimageoverlay
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${opencv_LIBRARIES}
  ${libsoup-2.4_LIBRARIES}
)

set_property(TARGET kmsimageoverlay
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}/../../..
    ${gstreamer-1.0_INCLUDE_DIRS}
    ${opencv_INCLUDE_DIRS}
    ${libsoup-2.4_INCLUDE_DIRS}
)

install(
  TARGETS kmsimageoverlay
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
