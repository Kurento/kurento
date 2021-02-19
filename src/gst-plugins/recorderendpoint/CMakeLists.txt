cmake_minimum_required(VERSION 2.8)

set(KMS_RECORDERENDPOINT_SOURCES
  kmsbasemediamuxer.c
  kmsavmuxer.c
  kmsksrmuxer.c
  kmsrecorderendpoint.c
)

set(KMS_RECORDERENDPOINT_HEADERS
  kmsbasemediamuxer.h
  kmsavmuxer.h
  kmsksrmuxer.h
  kmsrecorderendpoint.h
)

set(KMS_RECORDERENDPOINT_ENUM_HEADERS
  kmsrecordergapsfixmethod.h
)

list(APPEND KMS_RECORDERENDPOINT_HEADERS ${KMS_RECORDERENDPOINT_ENUM_HEADERS})
add_glib_enumtypes(
  KMS_RECORDERENDPOINT_SOURCES
  KMS_RECORDERENDPOINT_HEADERS
  kms-recorder-enumtypes
  KMS
  ${KMS_RECORDERENDPOINT_ENUM_HEADERS}
)

add_library(recorderendpoint MODULE ${KMS_RECORDERENDPOINT_SOURCES} ${KMS_RECORDERENDPOINT_HEADERS})
if(SANITIZERS_ENABLED)
  add_sanitizers(recorderendpoint)
endif()

set_property (TARGET recorderendpoint
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_BINARY_DIR}
    ${CMAKE_CURRENT_BINARY_DIR}/../../..
    ${gstreamer-1.5_INCLUDE_DIRS}
    ${KmsGstCommons_INCLUDE_DIRS}
)

target_link_libraries(recorderendpoint
  ${KmsGstCommons_LIBRARIES}
  ${gstreamer-1.5_LIBRARIES}
  ${gstreamer-base-1.5_LIBRARIES}
  ${gstreamer-app-1.5_LIBRARIES}
  ${gstreamer-pbutils-1.5_LIBRARIES}
)

install(
  TARGETS recorderendpoint
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
