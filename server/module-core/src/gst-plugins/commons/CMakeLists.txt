set(KMS_SDP_UTILS_SOURCES
  sdp_utils.c
)

set(KMS_SDP_UTILS_HEADERS
  sdp_utils.h
)

add_library(sdputils ${KMS_SDP_UTILS_SOURCES} ${KMS_SDP_UTILS_HEADERS})
if(SANITIZERS_ENABLED)
  add_sanitizers(sdputils)
endif()

set_property(TARGET sdputils
  PROPERTY INCLUDE_DIRECTORIES
    ${gstreamer-1.0_INCLUDE_DIRS}
    ${gstreamer-base-1.0_INCLUDE_DIRS}
    ${gstreamer-sdp-1.0_INCLUDE_DIRS}
    ${gstreamer-pbutils-1.0_INCLUDE_DIRS}
)

target_link_libraries(sdputils
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-base-1.0_LIBRARIES}
  ${gstreamer-sdp-1.0_LIBRARIES}
  ${gstreamer-pbutils-1.0_LIBRARIES}
)

set_target_properties(sdputils PROPERTIES
  PUBLIC_HEADER "${KMS_SDP_UTILS_HEADERS}"
  VERSION ${PROJECT_VERSION}
  SOVERSION ${PROJECT_VERSION_MAJOR}
)

set(CUSTOM_PREFIX "kurento")
set(INCLUDE_PREFIX "${CMAKE_INSTALL_INCLUDEDIR}/${CUSTOM_PREFIX}/commons")

install(
  TARGETS sdputils
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
  PUBLIC_HEADER DESTINATION ${INCLUDE_PREFIX}
)

add_library(kmsrefstruct kmsrefstruct.c kmsrefstruct.h)
if(SANITIZERS_ENABLED)
  add_sanitizers(kmsrefstruct)
endif()

set_property(TARGET kmsrefstruct
  PROPERTY INCLUDE_DIRECTORIES
    ${gstreamer-1.0_INCLUDE_DIRS}
)

target_link_libraries(kmsrefstruct
  ${gstreamer-1.0_LIBRARIES}
)

set_target_properties(kmsrefstruct PROPERTIES PUBLIC_HEADER kmsrefstruct.h)

install(
  TARGETS kmsrefstruct
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
  PUBLIC_HEADER DESTINATION ${INCLUDE_PREFIX}
)

set(KMS_UTILS
  kmsutils.c kmsutils.h
)

add_library(kmsutils ${KMS_UTILS})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmsutils)
endif()

set_property(TARGET kmsutils
  PROPERTY INCLUDE_DIRECTORIES
    ${gstreamer-1.0_INCLUDE_DIRS}
    ${gstreamer-base-1.0_INCLUDE_DIRS}
    ${gstreamer-sdp-1.0_INCLUDE_DIRS}
    ${gstreamer-pbutils-1.0_INCLUDE_DIRS}
    ${gstreamer-video-1.0_INCLUDE_DIRS}
    ${uuid_INCLUDE_DIRS}
)

set_target_properties(kmsutils PROPERTIES PUBLIC_HEADER kmsutils.h)

target_link_libraries(kmsutils
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-base-1.0_LIBRARIES}
  ${gstreamer-sdp-1.0_LIBRARIES}
  ${gstreamer-pbutils-1.0_LIBRARIES}
  ${gstreamer-video-1.0_LIBRARIES}
  ${uuid_LIBRARIES}
)

install(
  TARGETS kmsutils
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
  PUBLIC_HEADER DESTINATION ${INCLUDE_PREFIX}
)

set(KMS_COMMONS_SOURCES
  kmsrtcp.c
  kmsremb.c
  kmssdpsession.c
  kmsbasertpsession.c
  kmsirtpsessionmanager.c
  kmsirtpconnection.c
  kmsbasertpendpoint.c
  kmsbasesdpendpoint.c
  kmselement.c
  kmsloop.c
  kmsrecordingprofile.c
  kmshubport.c
  kmsbasehub.c
  kmsuriendpoint.c
  kmsbufferlacentymeta.c
  kmsserializablemeta.c
  kmsstats.c
  kmstreebin.c
  kmsdectreebin.c
  kmsenctreebin.c
  kmsparsetreebin.c
  kmsrtppaytreebin.c
  kmslist.c
)

set(KMS_COMMONS_HEADERS
  constants.h
  kmsrtcp.h
  kmsremb.h
  kmssdpsession.h
  kmsbasertpsession.h
  kmsirtpsessionmanager.h
  kmsirtpconnection.h
  kmsbasertpendpoint.h
  kmsbasesdpendpoint.h
  kmselement.h
  kmsloop.h
  kmsrecordingprofile.h
  kmshubport.h
  kmsbasehub.h
  kmsagnosticcaps.h
  kmsuriendpointstate.h
  kmsmediatype.h
  kmsuriendpoint.h
  kmsbufferlacentymeta.h
  kmsserializablemeta.h
  kmsstats.h
  kmstreebin.h
  kmsdectreebin.h
  kmsenctreebin.h
  kmsparsetreebin.h
  kmsrtppaytreebin.h
  kmslist.h
)

set(ENUM_HEADERS
  kmsuriendpointstate.h
  kmsrecordingprofile.h
  kmsmediatype.h
  kmsfiltertype.h
  kmselementpadtype.h
  kmsmediastate.h
  kmsconnectionstate.h
  gstsdpdirection.h
)

list(APPEND KMS_COMMONS_HEADERS ${ENUM_HEADERS})

add_glib_enumtypes(KMS_COMMONS_SOURCES KMS_COMMONS_HEADERS kms-core-enumtypes KMS ${ENUM_HEADERS})
add_glib_marshal(KMS_COMMONS_SOURCES KMS_COMMONS_HEADERS kms-core-marshal __kms_core_marshal)

add_library(kmsgstcommons SHARED ${KMS_COMMONS_SOURCES} ${KMS_COMMONS_HEADERS})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmsgstcommons)
endif()

set_property(TARGET kmsgstcommons
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_BINARY_DIR}
    ${CMAKE_CURRENT_BINARY_DIR}/../../..
    ${gstreamer-1.0_INCLUDE_DIRS}
    ${gstreamer-base-1.0_INCLUDE_DIRS}
    ${gstreamer-sdp-1.0_INCLUDE_DIRS}
    ${gstreamer-pbutils-1.0_INCLUDE_DIRS}
    ${gstreamer-rtp-1.0_INCLUDE_DIRS}
)

target_link_libraries(kmsgstcommons
  kmsutils
  sdputils
  kmsrefstruct
  kmssdpagent
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-base-1.0_LIBRARIES}
  ${gstreamer-sdp-1.0_LIBRARIES}
  ${gstreamer-pbutils-1.0_LIBRARIES}
  ${gstreamer-rtp-1.0_LIBRARIES}
)

set_target_properties(kmsgstcommons PROPERTIES PUBLIC_HEADER "${KMS_COMMONS_HEADERS}")

set_target_properties(kmsgstcommons PROPERTIES VERSION ${PROJECT_VERSION} SOVERSION ${PROJECT_VERSION_MAJOR})

install(
  TARGETS kmsgstcommons
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

configure_file(kmsgstcommons.pc.in
  ${PROJECT_BINARY_DIR}/kmsgstcommons.pc
  @ONLY
)
install(
  FILES ${PROJECT_BINARY_DIR}/kmsgstcommons.pc
  DESTINATION ${CMAKE_INSTALL_LIBDIR}/pkgconfig
)

foreach(HEADER ${KMS_COMMONS_HEADERS})
  list(APPEND FIND_HEADERS commons/${HEADER})
endforeach()

set(REQUIRED_LIBS
  "gstreamer-1.0 ${GST_REQUIRED}"
  "gstreamer-base-1.0 ${GST_REQUIRED}"
  "gstreamer-sdp-1.0 ${GST_REQUIRED}"
  "gstreamer-pbutils-1.0 ${GST_REQUIRED}"
)

configure_file(FindKmsGstCommons.cmake.in
  ${PROJECT_BINARY_DIR}/FindKmsGstCommons.cmake
  @ONLY
)
install(
  FILES ${PROJECT_BINARY_DIR}/FindKmsGstCommons.cmake
  DESTINATION ${CMAKE_INSTALL_DATAROOTDIR}/cmake/Kurento
)

add_subdirectory(sdpagent)
