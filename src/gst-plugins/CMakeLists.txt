set(GSTREAMER_PLUGIN_SAMPLE_SOURCES
  gstreamerpluginsample.c
  gstgstreamerpluginsample.h
  gstgstreamerpluginsample.cpp
)

add_library(gstgstreamerpluginsample MODULE ${GSTREAMER_PLUGIN_SAMPLE_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(gstgstreamerpluginsample)
endif()

target_include_directories(gstgstreamerpluginsample SYSTEM
  PRIVATE
    ${GSTREAMER_INCLUDE_DIRS}
    ${GSTREAMER_VIDEO_INCLUDE_DIRS}
    ${OpenCV_INCLUDE_DIRS}
)

target_link_libraries(gstgstreamerpluginsample
  PRIVATE
    ${GSTREAMER_LIBRARIES}
    ${GSTREAMER_VIDEO_LIBRARIES}
    ${OpenCV_LIBRARIES}
)

install(
  TARGETS gstgstreamerpluginsample
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
