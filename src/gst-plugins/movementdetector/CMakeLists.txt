set(MOVEMENTDETECTOR_SOURCES
  movementdetector.c
  kmsmovementdetector.c kmsmovementdetector.h
)

add_library(kmsmovementdetector MODULE ${MOVEMENTDETECTOR_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmsmovementdetector)
endif()

target_link_libraries(kmsmovementdetector
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${OpenCV_LIBRARIES}
)

set_property(TARGET kmsmovementdetector
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}/../../..
    ${gstreamer-1.0_INCLUDE_DIRS}
    ${OpenCV_INCLUDE_DIRS}
)

install(
  TARGETS kmsmovementdetector
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
