set(FACEDETECTOR_SOURCES
  classifier.cpp
  classifier.hpp
  facedetector.cpp
  kmsfacedetector.cpp
  kmsfacedetector.hpp
)

add_library(facedetector MODULE ${FACEDETECTOR_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(facedetector)
endif()

target_include_directories(facedetector SYSTEM
  PRIVATE
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${OpenCV_INCLUDE_DIRS}
)

target_include_directories(facedetector
  PRIVATE
  ${CMAKE_CURRENT_BINARY_DIR}/../../..
)

target_link_libraries(facedetector
  PRIVATE
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${OpenCV_LIBRARIES}
  ${SOUP_LIBRARIES}
)

install(
  TARGETS facedetector
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
