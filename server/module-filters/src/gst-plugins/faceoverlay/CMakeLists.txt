set(FACEOVERLAY_SOURCES
  faceoverlay.c
  kmsfaceoverlay.c kmsfaceoverlay.h
)

add_library(kmsfaceoverlay MODULE ${FACEOVERLAY_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmsfaceoverlay)
endif()

add_dependencies(kmsfaceoverlay kmsimageoverlay kmsfacedetector)

target_link_libraries(kmsfaceoverlay
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${OpenCV_LIBRARIES}
)

set_property(TARGET kmsfaceoverlay
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}/../../..
    ${gstreamer-1.0_INCLUDE_DIRS}
    ${OpenCV_INCLUDE_DIRS}
)

install(
  TARGETS kmsfaceoverlay
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
