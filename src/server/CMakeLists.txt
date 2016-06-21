cmake_minimum_required (VERSION 2.8)

list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/CMake")

include (CodeGenerator)

add_subdirectory(implementation/HttpServer)

generate_code (
  MODELS ${CMAKE_CURRENT_SOURCE_DIR}/interface
  SERVER_IMPL_LIB_EXTRA_INCLUDE_DIRS
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
