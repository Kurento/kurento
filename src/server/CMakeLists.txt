cmake_minimum_required (VERSION 2.8)

list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/CMake")

include (CodeGenerator)

add_subdirectory(implementation/HttpServer)

generate_code (
  MODELS ${CMAKE_CURRENT_SOURCE_DIR}/interface
  SERVER_IMPL_LIB_EXTRA_INCLUDE_DIRS
      ${CMAKE_CURRENT_SOURCE_DIR}/implementation
  SERVER_IMPL_LIB_EXTRA_LIBRARIES kmshttpep
  SERVER_STUB_DESTINATION ${CMAKE_CURRENT_SOURCE_DIR}/implementation/objects
)
