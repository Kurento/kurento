set(FACEOVERLAY_SOURCES
  faceoverlay.cpp
  kmsfaceoverlay.cpp
  kmsfaceoverlay.hpp
)

add_library(faceoverlay MODULE ${FACEOVERLAY_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(faceoverlay)
endif()

add_dependencies(faceoverlay imageoverlay facedetector)

target_include_directories(faceoverlay SYSTEM
  PRIVATE
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${OpenCV_INCLUDE_DIRS}
)

target_include_directories(faceoverlay
  PRIVATE
  ${CMAKE_CURRENT_BINARY_DIR}/../../..
)

target_link_libraries(faceoverlay
  PRIVATE
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${OpenCV_LIBRARIES}
)

install(
  TARGETS faceoverlay
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
