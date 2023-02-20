set(MOVEMENTDETECTOR_SOURCES
  movementdetector.cpp
  kmsmovementdetector.cpp
  kmsmovementdetector.hpp
)

add_library(kmsmovementdetector MODULE ${MOVEMENTDETECTOR_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmsmovementdetector)
endif()

target_include_directories(kmsmovementdetector SYSTEM
  PRIVATE
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${OpenCV_INCLUDE_DIRS}
)

target_include_directories(kmsmovementdetector
  PRIVATE
  ${CMAKE_CURRENT_BINARY_DIR}/../../..
)

target_link_libraries(kmsmovementdetector
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${OpenCV_LIBRARIES}
)

install(
  TARGETS kmsmovementdetector
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
