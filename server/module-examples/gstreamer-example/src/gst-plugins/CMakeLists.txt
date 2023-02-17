set(GSTREAMER_EXAMPLE_SOURCES
  gstreamerexample.c
  gstgstreamerexample.h
  gstgstreamerexample.cpp
)

add_library(gstreamerexample MODULE ${GSTREAMER_EXAMPLE_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(gstreamerexample)
endif()

target_include_directories(gstreamerexample SYSTEM
  PRIVATE
  ${GSTREAMER_INCLUDE_DIRS}
  ${GSTREAMER_VIDEO_INCLUDE_DIRS}
  ${OpenCV_INCLUDE_DIRS}
)

target_link_libraries(gstreamerexample
  PRIVATE
  ${GSTREAMER_LIBRARIES}
  ${GSTREAMER_VIDEO_LIBRARIES}
  ${OpenCV_LIBRARIES}
)

install(
  TARGETS gstreamerexample
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
