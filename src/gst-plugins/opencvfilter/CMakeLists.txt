set(OPENCV_FILTER_SOURCES
  opencvfilter.c
  kmsopencvfilter.cpp kmsopencvfilter.h
)

add_library(opencvfilter MODULE ${OPENCV_FILTER_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(opencvfilter)
endif()

target_link_libraries(opencvfilter
  ${gstreamer-1.5_LIBRARIES}
  ${gstreamer-video-1.5_LIBRARIES}
  ${opencv_LIBRARIES}
  kmsfiltersimpl
)

set_property(TARGET opencvfilter
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}/../../..
    ${CMAKE_CURRENT_SOURCE_DIR}/../../server/implementation/objects/
    ${gstreamer-1.5_INCLUDE_DIRS}
    ${opencv_INCLUDE_DIRS}
    ${KMSCORE_INCLUDE_DIRS}
)

install(
  TARGETS opencvfilter
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
