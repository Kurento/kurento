set(OPENCVFILTER_SOURCES
  kmsopencvfilter.cpp
  kmsopencvfilter.hpp
  opencvfilter.cpp
)

add_library(opencvfilter MODULE ${OPENCVFILTER_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(opencvfilter)
endif()

target_include_directories(opencvfilter SYSTEM
  PRIVATE
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${OpenCV_INCLUDE_DIRS}
  ${KmsJsonRpc_INCLUDE_DIRS}
  ${KMSCORE_INCLUDE_DIRS}
)

target_include_directories(opencvfilter
  PRIVATE
  ${CMAKE_CURRENT_BINARY_DIR}/../../..
  ${CMAKE_CURRENT_SOURCE_DIR}/../../server/implementation/objects/
)

target_link_libraries(opencvfilter
  PRIVATE
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${OpenCV_LIBRARIES}
  kmsfiltersimpl
)

install(
  TARGETS opencvfilter
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
