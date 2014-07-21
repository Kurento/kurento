cmake_minimum_required(VERSION 2.8)

include(GLibHelpers)

set (SOUP_REQUIRED 2.40)
set (NICE_REQUIRED 0.1.4)

pkg_check_modules(SOUP REQUIRED libsoup-2.4>=${SOUP_REQUIRED})
pkg_check_modules(NICE REQUIRED nice>=${NICE_REQUIRED})

set(KMS_ELEMENTS_SOURCES
  kmselements.c
  kmshttpendpoint.h
  kmshttpendpoint.c
  kmsplayerendpoint.c
  kmsrecorderendpoint.c
  kmsrtpendpoint.c
)

set(KMS_ELEMENTS_HEADERS
  kmshttpendpoint.h
  kmshttpendpointmethod.h
  kmsplayerendpoint.h
  kmsrecorderendpoint.h
  kmsrtpendpoint.h
)

set(ENUM_HEADERS
  kmshttpendpointmethod.h
)

add_glib_enumtypes(KMS_ELEMENTS_SOURCES KMS_ELEMENTS_HEADERS kms-elements-enumtypes KMS ${ENUM_HEADERS})

add_library(${LIBRARY_NAME}plugins MODULE ${KMS_ELEMENTS_SOURCES} ${KMS_ELEMENTS_HEADERS})

include_directories(
  ${KMSGSTCOMMONS_INCLUDE_DIRS}
  ${GSTREAMER_INCLUDE_DIRS}
  ${GSTREAMER_BASE_SOURCE_DIRS}
  ${GSTREAMER_SDP_SOURCE_DIRS}
  ${GSTREAMER_PBUTILS_SOURCE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${CMAKE_CURRENT_BINARY_DIR}
  ${SOUP_INCLUDE_DIRS}
  ${NICE_INCLUDE_DIRS}
)

target_link_libraries(${LIBRARY_NAME}plugins
  ${KMSGSTCOMMONS_LIBRARIES}
  ${GSTREAMER_LIBRARIES}
  ${GSTREAMER_BASE_LIBRARIES}
  ${GSTREAMER_SDP_LIBRARIES}
  ${GSTREAMER_PBUTILS_LIBRARIES}
  ${SOUP_LIBRARIES}
  ${NICE_LIBRARIES}
)

set (CMAKE_INSTALL_MODULESDIR ${CMAKE_INSTALL_LIBDIR}/kurento/gst-plugins)

install(
  TARGETS ${LIBRARY_NAME}plugins
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_MODULESDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
