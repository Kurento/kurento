cmake_minimum_required (VERSION 2.8)

# Add CMake modules provided by the project.
list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/cmake")

include (CodeGenerator)

generate_code (
  MODELS ${CMAKE_CURRENT_SOURCE_DIR}/interface
  SERVER_STUB_DESTINATION ${CMAKE_CURRENT_SOURCE_DIR}/implementation/objects
)
