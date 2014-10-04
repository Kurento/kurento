include_directories(
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${gstreamer-video-1.0_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${opencv_INCLUDE_DIRS}
)

set(FACEOVERLAY_SOURCES
  faceoverlay.c
  kmsfaceoverlay.c kmsfaceoverlay.h
)

add_library(faceoverlay MODULE ${FACEOVERLAY_SOURCES})

add_dependencies(faceoverlay imageoverlay facedetector)

target_link_libraries(faceoverlay
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${opencv_LIBRARIES}
)

install(
  TARGETS faceoverlay
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
