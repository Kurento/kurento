set(MOVEMENTDETECTOR_SOURCES
  movementdetector.cpp
  kmsmovementdetector.cpp
  kmsmovementdetector.hpp
)

add_library(movementdetector MODULE ${MOVEMENTDETECTOR_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(movementdetector)
endif()

target_include_directories(movementdetector SYSTEM
  PRIVATE
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${OpenCV_INCLUDE_DIRS}
)

target_include_directories(movementdetector
  PRIVATE
  ${CMAKE_CURRENT_BINARY_DIR}/../../..
)

target_link_libraries(movementdetector
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${OpenCV_LIBRARIES}
)

install(
  TARGETS movementdetector
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
