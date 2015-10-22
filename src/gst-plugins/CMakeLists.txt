include_directories(
  ${KmsGstCommons_INCLUDE_DIRS}
  ${gstreamer-1.5_INCLUDE_DIRS}
  ${gstreamer-video-1.5_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${opencv_INCLUDE_DIRS}
)

set(KMS_DATA_CHANNEL_EXAMPLE_SOURCES
  kmsdatachannelexample.c
  kmsshowdata.c
  kmsshowdata.h
  kmssenddata.c
  kmssenddata.h
  kmsfacedetectormetadata.h
  kmsfacedetectormetadata.c
  kmsimageoverlaymetadata.h
  kmsimageoverlaymetadata.c
)

add_library(kmsdatachannelexample MODULE ${KMS_DATA_CHANNEL_EXAMPLE_SOURCES})

target_link_libraries(kmsdatachannelexample
  ${KmsGstCommons_LIBRARIES}
  ${gstreamer-1.5_LIBRARIES}
  ${gstreamer-video-1.5_LIBRARIES}
  ${opencv_LIBRARIES}
)

install(
  TARGETS kmsdatachannelexample
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
