set(LOGOOVERLAY_SOURCES
  logooverlay.c
  kmslogooverlay.c kmslogooverlay.h
)

add_library(logooverlay MODULE ${LOGOOVERLAY_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(logooverlay)
endif()

target_link_libraries(logooverlay
  ${gstreamer-1.5_LIBRARIES}
  ${gstreamer-video-1.5_LIBRARIES}
  ${opencv_LIBRARIES}
  ${libsoup-2.4_LIBRARIES}
)

set_property(TARGET logooverlay
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}/../../..
    ${gstreamer-1.5_INCLUDE_DIRS}
    ${opencv_INCLUDE_DIRS}
    ${libsoup-2.4_INCLUDE_DIRS}
)

install(
  TARGETS logooverlay
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
