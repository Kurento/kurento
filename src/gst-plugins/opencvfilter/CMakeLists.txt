set(OPENCV_FILTER_SOURCES
  opencvfilter.c
  kmsopencvfilter.cpp kmsopencvfilter.h
)

add_library(kmsopencvfilter MODULE ${OPENCV_FILTER_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmsopencvfilter)
endif()

target_link_libraries(kmsopencvfilter
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${opencv_LIBRARIES}
  kmsfiltersimpl
)

set_property(TARGET kmsopencvfilter
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}/../../..
    ${CMAKE_CURRENT_SOURCE_DIR}/../../server/implementation/objects/
    ${gstreamer-1.0_INCLUDE_DIRS}
    ${opencv_INCLUDE_DIRS}
    ${KMSCORE_INCLUDE_DIRS}
)

install(
  TARGETS kmsopencvfilter
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
