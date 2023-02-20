set(IMAGEOVERLAY_SOURCES
  imageoverlay.cpp
  kmsimageoverlay.cpp
  kmsimageoverlay.hpp
)

add_library(kmsimageoverlay MODULE ${IMAGEOVERLAY_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmsimageoverlay)
endif()

target_include_directories(kmsimageoverlay SYSTEM
  PRIVATE
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${OpenCV_INCLUDE_DIRS}
  ${libsoup-2.4_INCLUDE_DIRS}
)

target_include_directories(kmsimageoverlay
  PRIVATE
  ${CMAKE_CURRENT_BINARY_DIR}/../../..
)

target_link_libraries(kmsimageoverlay
  PRIVATE
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${OpenCV_LIBRARIES}
  ${libsoup-2.4_LIBRARIES}
)

install(
  TARGETS kmsimageoverlay
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
