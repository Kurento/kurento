set(LOGOOVERLAY_SOURCES
  logooverlay.cpp
  kmslogooverlay.cpp
  kmslogooverlay.hpp
)

add_library(kmslogooverlay MODULE ${LOGOOVERLAY_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmslogooverlay)
endif()

target_include_directories(kmslogooverlay SYSTEM
  PRIVATE
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${OpenCV_INCLUDE_DIRS}
  ${libsoup-2.4_INCLUDE_DIRS}
)

target_include_directories(kmslogooverlay
  PRIVATE
  ${CMAKE_CURRENT_BINARY_DIR}/../../..
)

target_link_libraries(kmslogooverlay
  PRIVATE
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${OpenCV_LIBRARIES}
  ${libsoup-2.4_LIBRARIES}
)

install(
  TARGETS kmslogooverlay
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
