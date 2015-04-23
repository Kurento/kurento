include_directories(
  ${gstreamer-1.5_INCLUDE_DIRS}
  ${gstreamer-video-1.5_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${opencv_INCLUDE_DIRS}
  ${SOUP_INCLUDE_DIRS}
)

set(FACEDETECTOR_SOURCES
  facedetector.c
  kmsfacedetector.c kmsfacedetector.h
  classifier.h classifier.cpp
)

add_library(facedetector MODULE ${FACEDETECTOR_SOURCES})

target_link_libraries(facedetector
  ${gstreamer-1.5_LIBRARIES}
  ${gstreamer-video-1.5_LIBRARIES}
  ${opencv_LIBRARIES}
  ${SOUP_LIBRARIES}
)

install(
  TARGETS facedetector
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
