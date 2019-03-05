cmake_minimum_required(VERSION 2.8)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DHAVE_INTTYPES_H")

include(GLibHelpers)

SET(HTTP_EP_SOURCES
  KmsHttpEPServer.cpp
  KmsHttpPost.cpp
  HttpEndPointServer.cpp
)

SET(HTTP_EP_HEADERS
  KmsHttpEPServer.h
  KmsHttpPost.h
  HttpEndPointServer.hpp
)

set(ENUM_HEADERS
  kmshttpendpointaction.h
)

set(GEN_HEADERS
  kms-enumtypes.h
)

add_glib_marshal(HTTP_EP_SOURCES HTTP_EP_HEADERS http-marshal http_marshal)
add_glib_enumtypes(HTTP_EP_SOURCES HTTP_EP_HEADERS http-enumtypes KMS ${ENUM_HEADERS})

add_library(kmshttpep ${HTTP_EP_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmshttpep)
endif()

set_target_properties(kmshttpep PROPERTIES
  PUBLIC_HEADER "${HTTP_EP_HEADERS}"
  COMPILE_FLAGS "-Wno-deprecated-declarations"
)

set_property (TARGET kmshttpep
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${libsoup-2.4_INCLUDE_DIRS}
    ${KmsGstCommons_INCLUDE_DIRS}
    ${gstreamer-1.5_INCLUDE_DIRS}
)

target_link_libraries(kmshttpep
  ${glibmm-2.4_LIBRARIES}
  ${gstreamer-1.5_LIBRARIES}
  ${libsoup-2.4_LIBRARIES}
  ${uuid_LIBRARIES}
  ${gio-2.0_LIBRARIES}
  ${nice_LIBRARIES}
  ${KmsGstCommons_LIBRARIES}
)
