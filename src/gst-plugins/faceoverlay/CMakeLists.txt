include_directories(
  ${GSTREAMER_INCLUDE_DIRS}
  ${GSTREAMER_VIDEO_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${OPENCV_INCLUDE_DIRS}
)

set(FACEOVERLAY_SOURCES
  faceoverlay.c
  kmsfaceoverlay.c kmsfaceoverlay.h
)

add_library(faceoverlay MODULE ${FACEOVERLAY_SOURCES})

add_dependencies(faceoverlay imageoverlay facedetector)

target_link_libraries(faceoverlay
  ${GSTREAMER_LIBRARIES}
  ${GSTREAMER_VIDEO_LIBRARIES}
  ${OPENCV_LIBRARIES}
)

install(
  TARGETS faceoverlay
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
