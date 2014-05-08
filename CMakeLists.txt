add_subdirectory(vp8parse)
add_subdirectory(filters)

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
)

set(KMS_PLUGIN_SOURCES
  kmsplugin.c
  kmsloop.c kmsloop.h
  kmsconfcontroller.c kmsconfcontroller.h
  kmsrecordingprofile.h kmsrecordingprofile.c
  kmsutils.c kmsutils.h
  sdp_utils.c sdp_utils.h
  kmsagnosticbin.c kmsagnosticbin.h
  kmselement.c kmselement.h
  kmsbasesdpendpoint.c kmsbasesdpendpoint.h
  kmsbasertpendpoint.c kmsbasertpendpoint.h
  kmsrtpendpoint.c kmsrtpendpoint.h
  kmsautomuxerbin.c kmsautomuxerbin.h
  kmsrecorderendpoint.c kmsrecorderendpoint.h
  kmsfilterelement.c kmsfilterelement.h
  kmsplayerendpoint.c kmsplayerendpoint.h
  kmsuriendpoint.c kmsuriendpoint.h
  kmswebrtcendpoint.c kmswebrtcendpoint.h
  kmshttpendpoint.c kmshttpendpoint.h
  kmsmixerport.c kmsmixerport.h
  kmsbasehub.c kmsbasehub.h
  kmsdispatcheronetomany.c kmsdispatcheronetomany.h
  gstsctp.c gstsctp.h
  gstsctpbasesink.c gstsctpbasesink.h
  gstsctpclientsink.c gstsctpclientsink.h
  gstsctpserversrc.c gstsctpserversrc.h
  kmscompositemixer.c kmscompositemixer.h
  kmsdispatcher.c kmsdispatcher.h
  kmsaudiomixer.c kmsaudiomixer.h
)

include(GLibHelpers)

set(ENUM_HEADERS
  kmsuriendpointstate.h
  kmshttpendpointmethod.h
  kmsrecordingprofile.h
  kmsmediatype.h
)

add_glib_enumtypes(KMS_PLUGIN_SOURCES kms-enumtypes KMS ${ENUM_HEADERS})
add_glib_marshal(KMS_PLUGIN_SOURCES kms-marshal __kms_marshal)

add_library(kmsplugin MODULE ${KMS_PLUGIN_SOURCES})

target_link_libraries(kmsplugin
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
  LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
