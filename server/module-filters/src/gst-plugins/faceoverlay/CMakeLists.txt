set(FACEOVERLAY_SOURCES
  faceoverlay.cpp
  kmsfaceoverlay.cpp
  kmsfaceoverlay.hpp
)

add_library(kmsfaceoverlay MODULE ${FACEOVERLAY_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmsfaceoverlay)
endif()

add_dependencies(kmsfaceoverlay kmsimageoverlay kmsfacedetector)

target_include_directories(kmsfaceoverlay SYSTEM
  PRIVATE
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${OpenCV_INCLUDE_DIRS}
)

target_include_directories(kmsfaceoverlay
  PRIVATE
  ${CMAKE_CURRENT_BINARY_DIR}/../../..
)

target_link_libraries(kmsfaceoverlay
  PRIVATE
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${OpenCV_LIBRARIES}
)

install(
  TARGETS kmsfaceoverlay
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
