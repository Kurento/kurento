include_directories(
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${CMAKE_CURRENT_BINARY_DIR}
  ${GSTREAMER_INCLUDE_DIRS}
  ${GSTREAMER_BASE_SOURCE_DIRS}
  ${GSTREAMER_SDP_SOURCE_DIRS}
  ${GSTREAMER_PBUTILS_SOURCE_DIRS}
)

set(KMS_SDP_UTILS
  sdp_utils.c sdp_utils.h
)

add_library(sdputils ${KMS_SDP_UTILS})

target_link_libraries(sdputils
  ${GSTREAMER_LIBRARIES}
  ${GSTREAMER_BASE_LIBRARIES}
  ${GSTREAMER_SDP_LIBRARIES}
  ${GSTREAMER_PBUTILS_LIBRARIES}
)

install(
  TARGETS sdputils
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_MODULESDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)

set(KMS_UTILS
  kmsutils.c kmsutils.h
)

add_library(kmsutils ${KMS_UTILS})

target_link_libraries(kmsutils
  ${GSTREAMER_LIBRARIES}
  ${GSTREAMER_BASE_LIBRARIES}
  ${GSTREAMER_SDP_LIBRARIES}
  ${GSTREAMER_PBUTILS_LIBRARIES}
)

install(
  TARGETS kmsutils
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_MODULESDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)

set(KMS_COMMONS_SOURCES kmsbasertpendpoint.c
                        kmsbasesdpendpoint.c
                        kmsconfcontroller.c
                        kmselement.c
                        kmsloop.c
                        kmsrecordingprofile.c
                        kmsmixerport.c
                        kmsbasehub.c)

set(KMS_COMMONS_HEADERS kmsbasertpendpoint.h
                        kmsbasesdpendpoint.h
                        kmsconfcontroller.h
                        kmselement.h
                        kmsloop.h
                        kmsrecordingprofile.h
                        kmsmixerport.h
                        kmsbasehub.h)

set(ENUM_HEADERS
  kmsuriendpointstate.h
  kmshttpendpointmethod.h
  kmsrecordingprofile.h
  kmsmediatype.h
)

add_glib_enumtypes(KMS_COMMONS_HEADERS kms-enumtypes KMS ${ENUM_HEADERS})
add_glib_marshal(KMS_COMMONS_HEADERS kms-marshal __kms_marshal)

add_library(kmscommons SHARED ${KMS_COMMONS_SOURCES} ${KMS_COMMONS_HEADERS})

add_dependencies(kmscommons kmsutils sdputils)

target_link_libraries(kmscommons
  kmsutils
  sdputils
  ${GSTREAMER_LIBRARIES}
  ${GSTREAMER_BASE_LIBRARIES}
  ${GSTREAMER_SDP_LIBRARIES}
  ${GSTREAMER_PBUTILS_LIBRARIES}
)

set_target_properties(kmscommons PROPERTIES PUBLIC_HEADER "${KMS_COMMONS_HEADERS}")

set_target_properties(kmscommons PROPERTIES VERSION ${PROJECT_VERSION} SOVERSION ${PROJECT_VERSION_MAJOR})

set(CUSTOM_PREFIX "kurento")
set(INCLUDE_PREFIX "${CMAKE_INSTALL_INCLUDEDIR}/${CUSTOM_PREFIX}/commons")

install(
  TARGETS kmscommons
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
  PUBLIC_HEADER DESTINATION ${INCLUDE_PREFIX}
)

set(prefix ${CMAKE_INSTALL_PREFIX})
set(exec_prefix "\${prefix}")
set(libdir "\${exec_prefix}/${CMAKE_INSTALL_LIBDIR}")
set(includedir "\${prefix}/${CMAKE_INSTALL_INCLUDEDIR}/${CUSTOM_PREFIX}")
set(requires "gstreamer-1.0 gstreamer-base-1.0 gstreamer-sdp-1.0 gstreamer-pbutils-1.0")

configure_file(libkmscommons.pc.in ${CMAKE_CURRENT_BINARY_DIR}/libkmscommons.pc @ONLY)

install(FILES
  ${CMAKE_CURRENT_BINARY_DIR}/libkmscommons.pc
  DESTINATION ${CMAKE_INSTALL_LIBDIR}/pkgconfig
  )
