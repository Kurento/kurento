set(IMAGEOVERLAY_SOURCES
  imageoverlay.c
  kmsimageoverlay.c kmsimageoverlay.h
)

add_library(imageoverlay MODULE ${IMAGEOVERLAY_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(imageoverlay)
endif()

target_link_libraries(imageoverlay
  ${gstreamer-1.5_LIBRARIES}
  ${gstreamer-video-1.5_LIBRARIES}
  ${opencv_LIBRARIES}
  ${libsoup-2.4_LIBRARIES}
)

set_property(TARGET imageoverlay
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}/../../..
    ${gstreamer-1.5_INCLUDE_DIRS}
    ${opencv_INCLUDE_DIRS}
    ${libsoup-2.4_INCLUDE_DIRS}
)

install(
  TARGETS imageoverlay
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
