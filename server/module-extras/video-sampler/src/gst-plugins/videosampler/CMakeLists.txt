set(VIDEOSAMPLER_SOURCES
  videosampler.c
  kmsvideosampler.c
  kmsvideosampler.h
)

add_library(videosampler MODULE ${VIDEOSAMPLER_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(chroma)
endif()

target_include_directories(videosampler SYSTEM
  PRIVATE
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${KMSCORE_INCLUDE_DIRS}
  ${KmsGstCommons_INCLUDE_DIRS}
  ${GSTREAMER_INCLUDE_DIRS}
  ${GSTREAMER_VIDEO_INCLUDE_DIRS}
)

target_link_libraries(videosampler
  PRIVATE
  ${KmsGstCommons_LIBRARIES}
  ${GSTREAMER_LIBRARIES}
  ${GSTREAMER_VIDEO_LIBRARIES}
)

install(
  TARGETS videosampler
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
