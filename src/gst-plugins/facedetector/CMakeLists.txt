set(FACEDETECTOR_SOURCES
  facedetector.c
  kmsfacedetector.c kmsfacedetector.h
  classifier.h classifier.cpp
)

add_library(facedetector MODULE ${FACEDETECTOR_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(facedetector)
endif()

target_link_libraries(facedetector
  ${gstreamer-1.5_LIBRARIES}
  ${gstreamer-video-1.5_LIBRARIES}
  ${opencv_LIBRARIES}
  ${SOUP_LIBRARIES}
)

set_property(TARGET facedetector
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}/../../..
    ${gstreamer-1.5_INCLUDE_DIRS}
    ${opencv_INCLUDE_DIRS}
)

install(
  TARGETS facedetector
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
