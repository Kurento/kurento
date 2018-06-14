include_directories(
  ${GSTREAMER_INCLUDE_DIRS}
  ${GSTREAMER_VIDEO_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${OPENCV_INCLUDE_DIRS}
  ${TESSERACT_INCLUDE_DIRS}
)

set(PLATEDETECTOR_SOURCES
  platedetector.c
  kmsplatedetector.c kmsplatedetector.h
)

add_library(platedetector MODULE ${PLATEDETECTOR_SOURCES})

# This target uses 'setenv()', which is a POSIX.1-2001 extension
target_compile_definitions(platedetector PRIVATE _POSIX_C_SOURCE=200112L)

target_link_libraries(platedetector
  ${GSTREAMER_LIBRARIES}
  ${GSTREAMER_VIDEO_LIBRARIES}
  ${OPENCV_LIBRARIES}
  ${TESSERACT_LIBRARIES}
)

install(
  TARGETS platedetector
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)

install ( FILES
  tessdata/plateLanguage.traineddata

  DESTINATION ${CMAKE_INSTALL_DATAROOTDIR}/${PACKAGE}/tessdata/
)
