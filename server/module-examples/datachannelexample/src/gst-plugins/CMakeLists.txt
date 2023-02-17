set(DATACHANNELEXAMPLE_SOURCES
  datachannelexample.cpp
  kmsfacedetectormetadata.cpp
  kmsfacedetectormetadata.hpp
  kmsimageoverlaymetadata.cpp
  kmsimageoverlaymetadata.hpp
  kmssenddata.c
  kmssenddata.h
  kmsshowdata.c
  kmsshowdata.h
)

add_library(datachannelexample MODULE ${DATACHANNELEXAMPLE_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(datachannelexample)
endif()

target_include_directories(datachannelexample SYSTEM
  PRIVATE
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${KmsGstCommons_INCLUDE_DIRS}
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${gstreamer-video-1.0_INCLUDE_DIRS}
  ${OpenCV_INCLUDE_DIRS}
)

target_link_libraries(datachannelexample
  ${KmsGstCommons_LIBRARIES}
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${OpenCV_LIBRARIES}
)

install(
  TARGETS datachannelexample
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
