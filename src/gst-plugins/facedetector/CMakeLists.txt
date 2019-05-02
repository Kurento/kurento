set(FACEDETECTOR_SOURCES
  facedetector.c
  kmsfacedetector.c kmsfacedetector.h
  classifier.h classifier.cpp
)

add_library(kmsfacedetector MODULE ${FACEDETECTOR_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(facedetector)
endif()

target_link_libraries(kmsfacedetector
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${opencv_LIBRARIES}
  ${SOUP_LIBRARIES}
)

set_property(TARGET kmsfacedetector
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}/../../..
    ${gstreamer-1.0_INCLUDE_DIRS}
    ${opencv_INCLUDE_DIRS}
)

install(
  TARGETS kmsfacedetector
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
