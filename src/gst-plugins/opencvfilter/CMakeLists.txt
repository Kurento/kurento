include_directories(
  ${gstreamer-1.5_INCLUDE_DIRS}
  ${gstreamer-video-1.5_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${opencv_INCLUDE_DIRS}
  "${CMAKE_CURRENT_SOURCE_DIR}/../../server/implementation/objects/"
  ${KMSCORE_INCLUDE_DIRS}
)

set(OPENCV_FILTER_SOURCES
  opencvfilter.c
  kmsopencvfilter.cpp kmsopencvfilter.h
)

add_library(opencvfilter MODULE ${OPENCV_FILTER_SOURCES})

target_link_libraries(opencvfilter
  ${gstreamer-1.5_LIBRARIES}
  ${gstreamer-video-1.5_LIBRARIES}
  ${opencv_LIBRARIES}
)

install(
  TARGETS opencvfilter
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
