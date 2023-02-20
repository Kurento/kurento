set(FACEDETECTOR_SOURCES
  facedetector.cpp
  kmsfacedetector.cpp
  kmsfacedetector.hpp
)

add_library(kmsfacedetector MODULE ${FACEDETECTOR_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmsfacedetector)
endif()

target_include_directories(kmsfacedetector SYSTEM
  PRIVATE
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${OpenCV_INCLUDE_DIRS}
)

target_include_directories(kmsfacedetector
  PRIVATE
  ${CMAKE_CURRENT_BINARY_DIR}/../../..
)

target_link_libraries(kmsfacedetector
  PRIVATE
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${OpenCV_LIBRARIES}
  ${SOUP_LIBRARIES}
)

install(
  TARGETS kmsfacedetector
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
