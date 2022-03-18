set(LOGOOVERLAY_SOURCES
  logooverlay.c
  kmslogooverlay.c kmslogooverlay.h
)

add_library(kmslogooverlay MODULE ${LOGOOVERLAY_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmslogooverlay)
endif()

target_link_libraries(kmslogooverlay
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${OpenCV_LIBRARIES}
  ${libsoup-2.4_LIBRARIES}
)

set_property(TARGET kmslogooverlay
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}/../../..
    ${gstreamer-1.0_INCLUDE_DIRS}
    ${OpenCV_INCLUDE_DIRS}
    ${libsoup-2.4_INCLUDE_DIRS}
)

install(
  TARGETS kmslogooverlay
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
