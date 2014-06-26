include_directories(
  ${GSTREAMER_INCLUDE_DIRS}
  ${GSTREAMER_VIDEO_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${OPENCV_INCLUDE_DIRS}
  ${SOUP_INCLUDE_DIRS}
)

set(POINTERDETECTOR2_SOURCES
  pointerdetector2.c
  kmspointerdetector2.c kmspointerdetector2.h
)

add_library(pointerdetector2 MODULE ${POINTERDETECTOR2_SOURCES})

add_dependencies(pointerdetector2 kmsplugin kmscommons)

target_link_libraries(pointerdetector2
  kmscommons
  ${GSTREAMER_LIBRARIES}
  ${GSTREAMER_VIDEO_LIBRARIES}
  ${OPENCV_LIBRARIES}
  ${SOUP_LIBRARIES}
)

install(
  TARGETS pointerdetector2
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_MODULESDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
