set(FACEOVERLAY_SOURCES
  faceoverlay.c
  kmsfaceoverlay.c kmsfaceoverlay.h
)

add_library(faceoverlay MODULE ${FACEOVERLAY_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(faceoverlay)
endif()

add_dependencies(faceoverlay imageoverlay facedetector)

target_link_libraries(faceoverlay
  ${gstreamer-1.5_LIBRARIES}
  ${gstreamer-video-1.5_LIBRARIES}
  ${opencv_LIBRARIES}
)

set_property(TARGET faceoverlay
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}/../../..
    ${gstreamer-1.5_INCLUDE_DIRS}
    ${opencv_INCLUDE_DIRS}
)

install(
  TARGETS faceoverlay
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
