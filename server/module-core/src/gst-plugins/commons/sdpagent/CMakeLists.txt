set(KMS_SDP_AGENT_SOURCES
  kmssdpagentcommon.c
  kmssdpagent.c
  kmssdpmediahandler.c
  kmssdprtpmediahandler.c
  kmssdprtpavpfmediahandler.c
  kmssdprtpavpmediahandler.c
  kmssdprtpsavpmediahandler.c
  kmssdprtpsavpfmediahandler.c
  kmssdpsctpmediahandler.c
  kmsisdppayloadmanager.c
  kmssdppayloadmanager.c
  kmsisdpmediaextension.c
  kmsisdpsessionextension.c
  kmssdpsdesext.c
  kmssdpbasegroup.c
  kmssdpbundlegroup.c
  kmssdpmidext.c
  kmssdpgroupmanager.c
  kmssdprejectmediahandler.c
  kmssdpconnectionext.c
  kmssdpulpfecext.c
  kmssdpredundantext.c
  kmssdpmediadirext.c
)

set(KMS_SDP_AGENT_ENUM_HEADERS
  kmssdpagentstate.h
)

set(KMS_SDP_AGENT_HEADERS
  kmssdpagentcommon.h
  kmssdpagent.h
  kmssdpmediahandler.h
  kmssdprtpmediahandler.h
  kmssdprtpavpfmediahandler.h
  kmssdprtpavpmediahandler.h
  kmssdprtpsavpmediahandler.h
  kmssdprtpsavpfmediahandler.h
  kmssdpsctpmediahandler.h
  kmsisdppayloadmanager.h
  kmssdppayloadmanager.h
  kmsisdpmediaextension.h
  kmsisdpsessionextension.h
  kmssdpsdesext.h
  kmssdpbasegroup.h
  kmssdpbundlegroup.h
  kmssdpmidext.h
  kmssdpgroupmanager.h
  kmssdprejectmediahandler.h
  kmssdpconnectionext.h
  kmssdpulpfecext.h
  kmssdpredundantext.h
  kmssdpmediadirext.h
  ${KMS_SDP_AGENT_ENUM_HEADERS}
)

add_glib_enumtypes (
  KMS_SDP_AGENT_SOURCES
  KMS_SDP_AGENT_HEADERS
  kms-sdp-agent-enumtypes
  KMS
  ${KMS_SDP_AGENT_ENUM_HEADERS}
)

add_glib_marshal(KMS_SDP_AGENT_SOURCES KMS_SDP_AGENT_HEADERS kms-sdp-agent-marshal __kms_sdp_agent_marshal)

add_library(kmssdpagent SHARED ${KMS_SDP_AGENT_SOURCES} ${KMS_SDP_AGENT_HEADERS})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmssdpagent)
endif()

add_dependencies(kmssdpagent sdputils kmsrefstruct)

set_target_properties(kmssdpagent PROPERTIES PUBLIC_HEADER "${KMS_SDP_AGENT_HEADERS}")

set_target_properties(kmssdpagent PROPERTIES VERSION ${PROJECT_VERSION} SOVERSION ${PROJECT_VERSION_MAJOR})

set_property (TARGET kmssdpagent
  PROPERTY INCLUDE_DIRECTORIES
  ${CMAKE_CURRENT_BINARY_DIR}/../../../../
  ${CMAKE_CURRENT_SOURCE_DIR}/../
  ${CMAKE_CURRENT_SOURCE_DIR}/../../
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${gstreamer-base-1.0_INCLUDE_DIRS}
  ${gstreamer-sdp-1.0_INCLUDE_DIRS}
  ${gstreamer-pbutils-1.0_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${CMAKE_CURRENT_BINARY_DIR}
)

set(SDP_AGENT_INCLUDE_PREFIX "${INCLUDE_PREFIX}/sdpagent")

target_link_libraries(kmssdpagent
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-base-1.0_LIBRARIES}
  ${gstreamer-sdp-1.0_LIBRARIES}
  ${gstreamer-pbutils-1.0_LIBRARIES}
  kmsutils
  sdputils
  kmsrefstruct
)

install(
  TARGETS kmssdpagent
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
  PUBLIC_HEADER DESTINATION ${SDP_AGENT_INCLUDE_PREFIX}
)
