set (CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC")

add_subdirectory(commons)

set(KMS_PLUGIN_SOURCES
  kmsplugin.c
  kmsagnosticbin.c
  kmsrtpendpoint.c
  kmsautomuxerbin.c
  kmsrecorderendpoint.c
  kmsfilterelement.c
  kmsplayerendpoint.c
  kmsuriendpoint.c
  kmswebrtcendpoint.c
  kmshttpendpoint.c
  kmsdispatcheronetomany.c
  gstsctp.c
  gstsctpbasesink.c
  gstsctpclientsink.c
  gstsctpserversrc.c
  kmscompositemixer.c
  kmsdispatcher.c
  kmsaudiomixer.c
  kmsaudiomixerbin.c
  kmsselectablemixer.c
  kmssctpconnection.c
  kmsrpcclient.c
)

set(KMS_PLUGIN_HEADERS
  kmsagnosticbin.h
  kmsrtpendpoint.h
  kmsautomuxerbin.h
  kmsrecorderendpoint.h
  kmsfilterelement.h
  kmsplayerendpoint.h
  kmsuriendpoint.h
  kmswebrtcendpoint.h
  kmshttpendpoint.h
  kmsdispatcheronetomany.h
  gstsctp.h
  gstsctpbasesink.h
  gstsctpclientsink.h
  gstsctpserversrc.h
  kmscompositemixer.h
  kmsdispatcher.h
  kmsaudiomixer.h
  kmsaudiomixerbin.h
  kmsselectablemixer.h
  kmssctpconnection.h
  kmsrpcclient.h
)

include(GLibHelpers)

add_library(kmsplugin MODULE ${KMS_PLUGIN_SOURCES} ${KMS_PLUGIN_HEADERS})

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
  ${GSTMARSHAL_INCLUDE_DIRS}
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
  ${GSTMARSHAL_LIBRARIES}
)

install(
  TARGETS kmsplugin
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_MODULESDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
