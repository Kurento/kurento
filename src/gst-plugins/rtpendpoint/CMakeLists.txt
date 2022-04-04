include(GNUInstallDirs) # CMAKE_INSTALL_*

set(CUSTOM_PREFIX "kurento")
set(INCLUDE_PREFIX "${CMAKE_INSTALL_INCLUDEDIR}/${CUSTOM_PREFIX}/rtpendpoint")

set(KMS_RTPENDPOINT_SOURCES
  kmsrtpbaseconnection.c
  kmsrtpconnection.c
  kmssrtpconnection.c
  kmsrtpsession.c
  kmssrtpsession.c
  kmsrtpendpoint.c
  kmssocketutils.c
  kmsrandom.c
)

set(KMS_RTPENDPOINT_HEADERS
  kmsrtpbaseconnection.h
  kmsrtpconnection.h
  kmssrtpconnection.h
  kmsrtpsession.h
  kmssrtpsession.h
  kmsrtpendpoint.h
  kmssocketutils.h
)

set(ENUM_HEADERS
  kmsrtpsdescryptosuite.h
)

list(APPEND KMS_RTPENDPOINT_HEADERS ${ENUM_HEADERS})
add_glib_enumtypes(KMS_RTPENDPOINT_SOURCES KMS_RTPENDPOINT_HEADERS kms-rtp-enumtypes KMS ${ENUM_HEADERS})

add_glib_marshal(KMS_RTPENDPOINT_SOURCES KMS_RTPENDPOINT_HEADERS kms-rtp-marshal __kms_rtp_marshal)

add_library(kmsrtpendpointlib SHARED ${KMS_RTPENDPOINT_SOURCES} ${KMS_RTPENDPOINT_HEADERS})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmsrtpendpointlib)
endif()

add_dependencies(kmsrtpendpointlib rtcpdemux)

set_property(TARGET kmsrtpendpointlib
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_BINARY_DIR}/../../..
    ${KmsGstCommons_INCLUDE_DIRS}
    ${gstreamer-1.0_INCLUDE_DIRS}
)

target_link_libraries(kmsrtpendpointlib
  ${KmsGstCommons_LIBRARIES}
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-base-1.0_LIBRARIES}
  ${gstreamer-sdp-1.0_LIBRARIES}
  ${gstreamer-pbutils-1.0_LIBRARIES}
  ${nice_LIBRARIES}
)

set_target_properties(kmsrtpendpointlib PROPERTIES PUBLIC_HEADER "${KMS_RTPENDPOINT_HEADERS}")
set_target_properties(kmsrtpendpointlib PROPERTIES VERSION ${PROJECT_VERSION} SOVERSION ${PROJECT_VERSION_MAJOR})

install(
  TARGETS kmsrtpendpointlib
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
  PUBLIC_HEADER DESTINATION ${INCLUDE_PREFIX}
)

foreach (HEADER ${KMS_RTPENDPOINT_HEADERS})
  list (APPEND FIND_HEADERS rtpendpoint/${HEADER})
endforeach ()

configure_file(
  FindKmsRtpEndpointLib.cmake.in
  ${CMAKE_BINARY_DIR}/FindKmsRtpEndpointLib.cmake
  @ONLY
)
install(
  FILES ${CMAKE_BINARY_DIR}/FindKmsRtpEndpointLib.cmake
  DESTINATION ${CMAKE_INSTALL_DATAROOTDIR}/cmake/Kurento
)

add_library(rtpendpoint MODULE kmsrtp.c)
if(SANITIZERS_ENABLED)
  add_sanitizers(rtpendpoint)
endif()

set_property (TARGET rtpendpoint
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}/../../..
    ${KmsGstCommons_INCLUDE_DIRS}
    ${gstreamer-1.0_INCLUDE_DIRS}
)

target_link_libraries(rtpendpoint
  kmsrtpendpointlib
)

install(
  TARGETS rtpendpoint
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
