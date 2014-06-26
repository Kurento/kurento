set (CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC")

add_subdirectory(commons)

set(KMS_PLUGIN_SOURCES
  kmsplugin.c
  kmsagnosticbin.c kmsagnosticbin.h
  kmsrtpendpoint.c kmsrtpendpoint.h
  kmsautomuxerbin.c kmsautomuxerbin.h
  kmsrecorderendpoint.c kmsrecorderendpoint.h
  kmsfilterelement.c kmsfilterelement.h
  kmsplayerendpoint.c kmsplayerendpoint.h
  kmsuriendpoint.c kmsuriendpoint.h
  kmswebrtcendpoint.c kmswebrtcendpoint.h
  kmshttpendpoint.c kmshttpendpoint.h
  kmsdispatcheronetomany.c kmsdispatcheronetomany.h
  gstsctp.c gstsctp.h
  gstsctpbasesink.c gstsctpbasesink.h
  gstsctpclientsink.c gstsctpclientsink.h
  gstsctpserversrc.c gstsctpserversrc.h
  kmscompositemixer.c kmscompositemixer.h
  kmsdispatcher.c kmsdispatcher.h
  kmsaudiomixer.c kmsaudiomixer.h
  kmsaudiomixerbin.c kmsaudiomixerbin.h
  kmsselectablemixer.c kmsselectablemixer.h
)

include(GLibHelpers)

add_library(kmsplugin MODULE ${KMS_PLUGIN_SOURCES})

add_dependencies(kmsplugin kmsdtls vp8parse kmscommons)

include_directories(
  ${GSTREAMER_INCLUDE_DIRS}
  ${GSTREAMER_BASE_SOURCE_DIRS}
  ${GSTREAMER_SDP_SOURCE_DIRS}
  ${GSTREAMER_PBUTILS_SOURCE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${CMAKE_CURRENT_BINARY_DIR}
  ${SOUP_INCLUDE_DIRS}
  ${NICE_INCLUDE_DIRS}
  ${GIO_INCLUDE_DIRS}
  ${SCTP_INCLUDE_DIRS}
  "${CMAKE_CURRENT_BINARY_DIR}/commons/"
  "${CMAKE_CURRENT_SOURCE_DIR}/commons/"
)

add_subdirectory(vp8parse)
add_subdirectory(filters)

target_link_libraries(kmsplugin
  kmscommons
  ${GSTREAMER_LIBRARIES}
  ${GSTREAMER_BASE_LIBRARIES}
  ${GSTREAMER_SDP_LIBRARIES}
  ${GSTREAMER_PBUTILS_LIBRARIES}
  ${SOUP_LIBRARIES}
  ${NICE_LIBRARIES}
  ${GIO_LIBRARIES}
  ${SCTP_LIBRARIES}
)

install(
  TARGETS kmsplugin
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_MODULESDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
