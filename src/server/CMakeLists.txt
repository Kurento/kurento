cmake_minimum_required(VERSION 2.8)

add_subdirectory(implementation/HttpServer)

set(KMS_ELEMENTS_IMPL_SOURCES
  implementation/CertificateManager.cpp
)

set(KMS_ELEMENTS_IMPL_HEADERS
  implementation/CertificateManager.hpp
)

include(CodeGenerator)
generate_code(
  MODELS ${CMAKE_CURRENT_SOURCE_DIR}/interface
  SERVER_IMPL_LIB_EXTRA_SOURCES
      ${KMS_ELEMENTS_IMPL_SOURCES}
  SERVER_IMPL_LIB_EXTRA_HEADERS
      ${KMS_ELEMENTS_IMPL_HEADERS}
  SERVER_IMPL_LIB_EXTRA_INCLUDE_DIRS
      ${CMAKE_CURRENT_BINARY_DIR}/..
      ${CMAKE_CURRENT_SOURCE_DIR}/../gst-plugins/webrtcendpoint
      ${CMAKE_CURRENT_SOURCE_DIR}/../gst-plugins
      ${KmsGstCommons_INCLUDE_DIRS}
      ${CMAKE_CURRENT_SOURCE_DIR}/implementation
      ${nice_INCLUDE_DIRS}
      ${openssl_INCLUDE_DIRS}
  SERVER_IMPL_LIB_EXTRA_LIBRARIES
      kmshttpep
      kmswebrtcendpointlib
      ${nice_LIBRARIES}
      ${KmsGstCommons_LIBRARIES}
      ${openssl_LIBRARIES}
  SERVER_STUB_DESTINATION ${CMAKE_CURRENT_SOURCE_DIR}/implementation/objects
)
