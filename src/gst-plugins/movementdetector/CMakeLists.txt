include_directories(
  ${gstreamer-1.5_INCLUDE_DIRS}
  ${gstreamer-video-1.5_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${opencv_INCLUDE_DIRS}
)

set(MOVEMENTDETECTOR_SOURCES
  movementdetector.c
  kmsmovementdetector.c kmsmovementdetector.h
)

add_library(movementdetector MODULE ${MOVEMENTDETECTOR_SOURCES})

target_link_libraries(movementdetector
  ${gstreamer-1.5_LIBRARIES}
  ${gstreamer-video-1.5_LIBRARIES}
  ${opencv_LIBRARIES}
)

install(
  TARGETS movementdetector
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
